package dev.balafini.factions.service;

import dev.balafini.factions.cache.FactionCache;
import dev.balafini.factions.model.faction.Faction;
import dev.balafini.factions.model.faction.FactionMember;
import dev.balafini.factions.repository.faction.FactionMemberRepository;
import dev.balafini.factions.repository.faction.FactionRepository;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static dev.balafini.factions.model.faction.Faction.FACTION_NAME_PATTERN;
import static dev.balafini.factions.model.faction.Faction.FACTION_TAG_PATTERN;

public class FactionService {

    private final FactionCache cache;
    private final FactionRepository factionRepository;
    private final FactionMemberRepository memberRepository;
    private final int maxFactionSize;

    public FactionService(FactionCache cache, FactionRepository factionRepository, FactionMemberRepository memberRepository, int maxFactionSize) {
        this.cache = cache;
        this.factionRepository = factionRepository;
        this.memberRepository = memberRepository;
        this.maxFactionSize = maxFactionSize;
    }

    public CompletionStage<Faction> createFaction(String name, String tag, UUID leaderId) {
        validateFactionParameters(name, tag);
        Faction newFaction = Faction.create(name, tag, leaderId);

        return validateFactionCreation(name, tag, leaderId).thenCompose(v -> {
            CompletionStage<Void> saveFaction = factionRepository.save(newFaction);
            CompletionStage<Void> saveLeader = memberRepository.save(newFaction.getLeader());
            return CompletableFuture.allOf(saveFaction.toCompletableFuture(), saveLeader.toCompletableFuture())
                    .thenApply(ignored -> {
                        cache.put(newFaction);
                        return newFaction;
                    });
        });
    }

    public CompletionStage<Void> disbandFaction(UUID factionId, UUID leaderId) {
        return this.findFactionById(factionId).thenCompose(optFaction -> {
            if (optFaction.isEmpty()) {
                return CompletableFuture.failedFuture(new IllegalArgumentException("A facção não foi encontrada."));
            }
            Faction faction = optFaction.get();
            if (!faction.getLeader().playerId().equals(leaderId)) {
                return CompletableFuture.failedFuture(new SecurityException("Apenas o líder da facção pode desbandar a facção."));
            }

            cache.invalidate(factionId);

            CompletionStage<Boolean> deleteFaction = factionRepository.deleteByFactionId(factionId);
            CompletionStage<Long> deleteMembers = memberRepository.deleteByFactionId(factionId);
            return CompletableFuture.allOf(deleteFaction.toCompletableFuture(), deleteMembers.toCompletableFuture())
                    .thenApply(ignored -> null);
        });
    }

    public CompletionStage<Void> addMember(UUID factionId, UUID newMemberId) {
        return this.findFactionById(factionId)
                .thenCompose(optFaction -> {
                    if (optFaction.isEmpty()) {
                        return CompletableFuture.failedFuture(new IllegalArgumentException("A facção para a qual você foi convidado não existe mais."));
                    }
                    Faction faction = optFaction.get();

                    if (faction.members().size() >= this.maxFactionSize) {
                        return CompletableFuture.failedFuture(new IllegalStateException("Esta facção está cheia."));
                    }

                    return memberRepository.findByPlayerId(newMemberId).thenCompose(optMember -> {
                        if (optMember.isPresent()) {
                            return CompletableFuture.failedFuture(new IllegalStateException("O jogador já está em uma facção."));
                        }

                        FactionMember newMember = FactionMember.create(newMemberId, FactionMember.FactionRole.RECRUIT, factionId);
                        cache.invalidate(factionId);
                        return memberRepository.save(newMember);
                    });
                });
    }


    public CompletionStage<Void> removeMember(UUID factionId, UUID requesterId, UUID targetId) {
        return this.findFactionById(factionId).thenCompose(optFaction -> {
            if (optFaction.isEmpty()) {
                return CompletableFuture.failedFuture(new IllegalArgumentException("Fação não encontrada."));
            }
            if (requesterId.equals(targetId)) {
                return CompletableFuture.failedFuture(new IllegalArgumentException("Você não pode remover a si mesmo da facção."));
            }
            Faction faction = optFaction.get();
            FactionMember requester = faction.getMember(requesterId);
            FactionMember target = faction.getMember(targetId);

            if (requester == null || target == null) {
                return CompletableFuture.failedFuture(new IllegalArgumentException("O jogador não é um membro da facção."));
            }
            if (target.role() == FactionMember.FactionRole.LEADER) {
                return CompletableFuture.failedFuture(new IllegalArgumentException("O lider da facção não pode ser removido por outro membro."));
            }
            if (!requester.role().isHigherThan(target.role())) {
                return CompletableFuture.failedFuture(new SecurityException("Você não tem permissão para remover este membro."));
            }

            cache.invalidate(factionId);
            return memberRepository.deleteByPlayerId(targetId).thenApply(ignored -> null);
        });
    }

    public CompletionStage<Optional<Faction>> findFactionById(UUID id) {
        return cache.getById(id);
    }

    public CompletionStage<Optional<Faction>> findFactionByName(String name) {
        return factionRepository.findByName(name)
                .thenCompose(optFaction -> optFaction
                        .map(faction -> findFactionById(faction.factionId()))
                        .orElse(CompletableFuture.completedFuture(Optional.empty()))
                );
    }

    public CompletionStage<Optional<Faction>> findFactionByTag(String tag) {
        return factionRepository.findByTag(tag)
                .thenCompose(optFaction -> optFaction
                        .map(faction -> findFactionById(faction.factionId()))
                        .orElse(CompletableFuture.completedFuture(Optional.empty()))
                );
    }

    public CompletionStage<Optional<Faction>> findFactionByPlayer(UUID playerId) {
        return memberRepository.findByPlayerId(playerId)
                .thenCompose(optMember -> optMember
                        .map(member -> findFactionById(member.factionId()))
                        .orElse(CompletableFuture.completedFuture(Optional.empty()))
                );
    }

    private CompletionStage<Void> validateFactionCreation(String name, String tag, UUID leaderId) {
        CompletionStage<Boolean> nameCheck = factionRepository.existsByName(name);
        CompletionStage<Boolean> tagCheck = factionRepository.existsByTag(tag);
        CompletionStage<Boolean> playerCheck = memberRepository.findByPlayerId(leaderId).thenApply(Optional::isPresent);

        return CompletableFuture.allOf(
                nameCheck.toCompletableFuture(),
                tagCheck.toCompletableFuture(),
                playerCheck.toCompletableFuture()
        ).thenAccept(ignored -> {
            if (nameCheck.toCompletableFuture().join()) {
                throw new IllegalArgumentException("O nome da facção já está em uso.");
            }
            if (tagCheck.toCompletableFuture().join()) {
                throw new IllegalArgumentException("A tag da facção já está em uso.");
            }
            if (playerCheck.toCompletableFuture().join()) {
                throw new IllegalArgumentException("O jogador já está em uma facção.");
            }
        });
    }

    private void validateFactionParameters(String name, String tag) {
        if (!name.matches(FACTION_NAME_PATTERN)) {
            throw new IllegalArgumentException("O nome da facção deve conter entre 6 e 16 caracteres alfanuméricos.");
        }
        if (!tag.matches(FACTION_TAG_PATTERN)) {
            throw new IllegalArgumentException("A tag da facção deve conter entre 3 e 4 caracteres alfanuméricos.");
        }
    }

}