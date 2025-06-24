package dev.balafini.factions.service.faction;

import dev.balafini.factions.cache.FactionCache;
import dev.balafini.factions.config.ConfigManager;
import dev.balafini.factions.exception.*;
import dev.balafini.factions.model.faction.Faction;
import dev.balafini.factions.model.faction.FactionMember;
import dev.balafini.factions.model.user.User;
import dev.balafini.factions.repository.faction.FactionMemberRepository;
import dev.balafini.factions.repository.faction.FactionRepository;
import dev.balafini.factions.service.user.UserService;

import java.util.List;
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
    private final UserService userService;
    private final ConfigManager config;

    public FactionService(FactionCache cache, FactionRepository factionRepository, FactionMemberRepository memberRepository, UserService userService, ConfigManager config) {
        this.cache = cache;
        this.factionRepository = factionRepository;
        this.memberRepository = memberRepository;
        this.userService = userService;
        this.config = config;
    }

    public CompletionStage<Faction> createFaction(String name, String tag, UUID leaderId) {
        try {
            validateFactionParameters(name, tag);
        } catch (InvalidFactionParametersException e) {
            return CompletableFuture.failedFuture(e);
        }

        return validateFactionCreation(name, tag, leaderId).thenCompose(_ ->
                userService.createUser(leaderId).thenCompose(leaderUser -> {
                    Faction newFaction = Faction.create(name, tag, leaderId, leaderUser.power(), leaderUser.maxPower(), leaderUser.getKdr());

                    CompletionStage<Void> saveFaction = factionRepository.save(newFaction);
                    CompletionStage<Void> saveLeader = memberRepository.save(newFaction.getLeader());

                    return CompletableFuture.allOf(saveFaction.toCompletableFuture(), saveLeader.toCompletableFuture())
                            .thenApply(_ -> {
                                cache.put(newFaction);
                                return newFaction;
                            });
                })
        );
    }

    public CompletionStage<Void> disbandFaction(UUID factionId, UUID leaderId) {
        return this.findFactionById(factionId).thenCompose(optFaction -> {
            if (optFaction.isEmpty()) {
                return CompletableFuture.failedFuture(new FactionNotFoundException("A facção não foi encontrada." ));
            }
            Faction faction = optFaction.get();
            if (!faction.getLeader().playerId().equals(leaderId)) {
                return CompletableFuture.failedFuture(new InsufficientPermissionException("Apenas o líder da facção pode excluir a facção." ));
            }

            cache.invalidate(factionId);

            CompletionStage<Boolean> deleteFaction = factionRepository.deleteByFactionId(factionId);
            CompletionStage<Long> deleteMembers = memberRepository.deleteByFactionId(factionId);

            return deleteFaction.thenCombine(deleteMembers, (_, __) -> null);
        });
    }

    public CompletionStage<Void> addMember(UUID factionId, UUID newMemberId) {
        return userService.createUser(newMemberId).thenCompose(user ->
                this.findFactionById(factionId).thenCompose(optFaction -> {
                    if (optFaction.isEmpty()) {
                        return CompletableFuture.failedFuture(new FactionNotFoundException("A facção não foi encontrada." ));
                    }
                    Faction faction = optFaction.get();
                    if (faction.members().size() >= config.maxFactionSize()) {
                        return CompletableFuture.failedFuture(new FactionFullException("Esta facção está cheia." ));
                    }

                    return memberRepository.findByPlayerId(newMemberId).thenCompose(optMember -> {
                        if (optMember.isPresent()) {
                            return CompletableFuture.failedFuture(new PlayerAlreadyInFactionException("O jogador já está em uma facção." ));
                        }

                        return factionRepository.updatePower(factionId, user.power(), user.maxPower())
                                .thenCompose(_ -> {
                                    cache.invalidate(factionId);
                                    FactionMember newMember = FactionMember.create(newMemberId, FactionMember.FactionRole.RECRUIT, factionId);
                                    return memberRepository.save(newMember);
                                });
                    });
                })
        );
    }


    public CompletionStage<Void> removeMember(UUID factionId, UUID requesterId, UUID targetId) {
        if (factionId == null) {
            return memberRepository.findByPlayerId(targetId).thenCompose(optMember -> {
                if (optMember.isEmpty()) {
                    return CompletableFuture.failedFuture(new PlayerNotInFactionException("Você não está em uma facção." ));
                }
                return removeMember(optMember.get().factionId(), requesterId, targetId);
            });
        }

        return userService.findUser(targetId).thenCompose(optUserToRemove -> {
            if (optUserToRemove.isEmpty()) {
                return CompletableFuture.failedFuture(new PlayerNotInFactionException("Dados do jogador a ser removido não encontrados." ));
            }
            User userToRemove = optUserToRemove.get();

            return this.findFactionById(factionId).thenCompose(optFaction -> {
                if (optFaction.isEmpty()) {
                    return CompletableFuture.failedFuture(new FactionNotFoundException("Facção não encontrada." ));
                }
                Faction faction = optFaction.get();

                FactionMember requester = faction.getMember(requesterId);
                FactionMember target = faction.getMember(targetId);

                if (target == null) {
                    return CompletableFuture.failedFuture(new PlayerNotInFactionException("O jogador não é um membro desta facção." ));
                }

                if (target.role() == FactionMember.FactionRole.LEADER) {
                    return CompletableFuture.failedFuture(new InsufficientPermissionException("O líder da facção não pode ser expulso." ));
                }

                if (!requesterId.equals(targetId)) {
                    if (requester == null) {
                        return CompletableFuture.failedFuture(new PlayerNotInFactionException("Você não é um membro desta facção." ));
                    }
                    if (!requester.role().isHigherThan(target.role())) {
                        return CompletableFuture.failedFuture(new InsufficientPermissionException("Você não tem permissão para expulsar este membro." ));
                    }
                }

                return factionRepository.updatePower(factionId, -userToRemove.power(), -userToRemove.maxPower())
                        .thenCompose(_ -> {
                            cache.invalidate(factionId);
                            return memberRepository.deleteByPlayerId(targetId).thenApply(ignored -> null);
                        });
            });
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

    public CompletionStage<Long> getFactionRank(Faction faction) {
        return factionRepository.countFactionsWithHigherKdr(faction.kdr())
                .thenApply(count -> count + 1);
    }

    public CompletionStage<List<Faction>> getTopFactionsByKdr(int limit) {
        return factionRepository.getTopKdrFactions(limit);
    }

    private CompletionStage<Void> validateFactionCreation(String name, String tag, UUID leaderId) {
        CompletionStage<Boolean> nameCheck = factionRepository.existsByName(name);
        CompletionStage<Boolean> tagCheck = factionRepository.existsByTag(tag);
        CompletionStage<Boolean> playerCheck = memberRepository.findByPlayerId(leaderId).thenApply(Optional::isPresent);

        return CompletableFuture.allOf(
                nameCheck.toCompletableFuture(),
                tagCheck.toCompletableFuture(),
                playerCheck.toCompletableFuture()
        ).thenCompose(_ -> {
            try {
                if (nameCheck.toCompletableFuture().join()) {
                    return CompletableFuture.failedFuture(new FactionAlreadyExistsException("O nome da facção já está em uso." ));
                }
                if (tagCheck.toCompletableFuture().join()) {
                    return CompletableFuture.failedFuture(new FactionAlreadyExistsException("A tag da facção já está em uso." ));
                }
                if (playerCheck.toCompletableFuture().join()) {
                    return CompletableFuture.failedFuture(new PlayerAlreadyInFactionException("O jogador já está em uma facção." ));
                }
                return CompletableFuture.completedFuture(null);
            } catch (Exception e) {
                return CompletableFuture.failedFuture(e);
            }
        });
    }

    private void validateFactionParameters(String name, String tag) {
        if (!name.matches(FACTION_NAME_PATTERN)) {
            throw new IllegalArgumentException("O nome da facção deve conter entre 6 e 16 caracteres alfanuméricos." );
        }
        if (!tag.matches(FACTION_TAG_PATTERN)) {
            throw new IllegalArgumentException("A tag da facção deve conter entre 3 e 4 caracteres alfanuméricos." );
        }
    }

}