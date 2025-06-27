package dev.balafini.factions.faction.service;

import dev.balafini.factions.faction.cache.FactionCache;
import dev.balafini.factions.config.ConfigManager;
import dev.balafini.factions.database.MongoManager;
import dev.balafini.factions.faction.cache.FactionMemberCache;
import dev.balafini.factions.faction.exception.*;
import dev.balafini.factions.faction.Faction;
import dev.balafini.factions.faction.FactionMember;
import dev.balafini.factions.user.User;
import dev.balafini.factions.faction.repository.FactionMemberRepository;
import dev.balafini.factions.faction.repository.FactionRepository;
import dev.balafini.factions.user.service.UserLifecycleService;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/*
    TODO: make demoteMember and promoteMember methods return the String representation of the new role
 */
public class FactionMemberService {

    private final UserLifecycleService userLifecycleService;
    private final FactionQueryService factionQueryService;
    private final FactionMemberRepository factionMemberRepository;
    private final FactionRepository factionRepository;

    private final FactionCache factionCache;
    private final FactionMemberCache factionMemberCache;

    private final MongoManager mongoManager;

    private final ConfigManager config;

    public FactionMemberService(
        UserLifecycleService userLifecycleService,
        FactionQueryService factionQueryService,
        FactionMemberRepository factionMemberRepository,
        FactionRepository factionRepository,
        FactionCache factionCache,
        FactionMemberCache factionMemberCache,
        MongoManager mongoManager,
        ConfigManager config
    ) {
        this.userLifecycleService = userLifecycleService;
        this.factionQueryService = factionQueryService;
        this.factionMemberRepository = factionMemberRepository;
        this.factionRepository = factionRepository;

        this.factionCache = factionCache;
        this.factionMemberCache = factionMemberCache;

        this.mongoManager = mongoManager;

        this.config = config;
    }

    public CompletableFuture<FactionMember> addMember(UUID factionId, UUID newMemberId, String newMemberDisplayName) {
        if (factionId == null || newMemberId == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("ID da facção e ID do novo membro não podem ser nulos"));
        }

        return userLifecycleService.getOrCreateUser(newMemberId, newMemberDisplayName)
            .thenCompose(user ->
                factionQueryService.findFactionById(factionId)
                    .thenCompose(optFaction -> {
                        if (optFaction.isEmpty()) {
                            throw new FactionNotFoundException("Facção não encontrada.");
                        }

                        Faction faction = optFaction.get();
                        if (faction.memberIds().size() >= config.maxFactionSize()) {
                            throw new FactionFullException("Esta facção já está cheia.");
                        }

                        if (faction.getMember(newMemberId) != null) {
                            throw new PlayerAlreadyInFactionException("O jogador já é um membro desta facção.");
                        }

                        return mongoManager.withTransaction(session -> {
                            FactionMember newMember = FactionMember.create(
                                newMemberId,
                                FactionMember.FactionRole.RECRUIT,
                                factionId
                            );

                            CompletableFuture<Void> saveMember = factionMemberRepository.insert(newMember, session);
                            CompletableFuture<Void> updatePower = factionRepository.updatePower(
                                factionId, user.power(), user.maxPower(), session
                            );

                            return CompletableFuture.allOf(saveMember, updatePower)
                                .thenCompose(_ -> {
                                    // Update faction state within transaction
                                    faction.addMember(newMemberId);
                                    return factionRepository.upsert(faction, session).toCompletableFuture();
                                })
                                .thenApply(_ -> newMember);
                        });
                    })
            )
            .toCompletableFuture();
    }

    public CompletableFuture<FactionMember> leaveFaction(UUID requesterId) {
        return factionQueryService.findFactionByPlayer(requesterId)
            .thenCompose(optFaction -> {
                if (optFaction.isEmpty()) {
                    throw new PlayerNotInFactionException("Você não está em uma facção.");
                }
                Faction faction = optFaction.get();
                FactionMember requester = faction.getMember(requesterId);

                if (requester.role() == FactionMember.FactionRole.LEADER) {
                    throw new InsufficientPermissionException("O líder não pode sair da facção. Use o comando /f desfazer para dissolver a facção.");
                }

                return userLifecycleService.getOrCreateUser(requesterId, "Unknown")
                    .thenCompose(user -> removeMemberInternal(faction, user))
                    .thenApply(_ -> requester);
            });
    }

    public CompletableFuture<FactionMember> kickMember(UUID requesterId, UUID targetId) {
        if (requesterId.equals(targetId)) {
            throw new IllegalArgumentException("Você não pode expulsar a si mesmo. Use /f leave para sair da facção.");
        }

        return factionQueryService.findFactionByPlayer(requesterId)
            .thenCompose(optFaction -> {
                if (optFaction.isEmpty()) {
                    throw new PlayerNotInFactionException("Você não está em uma facção.");
                }

                Faction faction = optFaction.get();

                FactionMember requester = faction.getMember(requesterId);
                FactionMember target = faction.getMember(targetId);

                if (target == null) {
                    throw new PlayerNotInFactionException("O jogador não é um membro desta facção.");
                }
                if (requester.role().isLowerThanOrEqualTo(target.role())) {
                    throw new InsufficientPermissionException("Você não tem permissão para expulsar este membro.");
                }
                if (target.role() == FactionMember.FactionRole.LEADER) {
                    throw new InsufficientPermissionException("O líder da facção não pode ser expulso.");
                }

                return userLifecycleService.getOrCreateUser(targetId, "Unknown")
                    .thenCompose(user -> removeMemberInternal(faction, user))
                    .thenApply(_ -> target);
            });
    }

    public CompletableFuture<FactionMember> promoteMember(UUID requesterId, UUID targetId) {
        return factionQueryService.findFactionByPlayer(requesterId)
            .thenCompose(optFaction -> {
                if (optFaction.isEmpty()) throw new PlayerNotInFactionException("Você não está em uma facção.");

                Faction faction = optFaction.get();
                FactionMember requester = faction.getMember(requesterId);
                FactionMember target = faction.getMember(targetId);

                if (target == null)
                    throw new PlayerNotInFactionException("O jogador não é um membro desta facção.");

                if (requester.role().isLowerThanOrEqualTo(target.role())) {
                    throw new InsufficientPermissionException("Você não tem permissão para promover este membro.");
                }

                FactionMember.FactionRole newRole = target.role().getNextRole()
                    .orElseThrow(() -> new IllegalStateException("Não é possível promover o membro além do nível de líder."));

                return updateMemberRole(target, newRole);
            });
    }

    public CompletionStage<FactionMember> demoteMember(UUID requesterId, UUID targetId) {
        return factionQueryService.findFactionByPlayer(requesterId)
            .thenCompose(optFaction -> {
                if (optFaction.isEmpty()) throw new PlayerNotInFactionException("Você não está em uma facção.");

                Faction faction = optFaction.get();
                FactionMember requester = faction.getMember(requesterId);
                FactionMember target = faction.getMember(targetId);

                if (target == null)
                    throw new PlayerNotInFactionException("O jogador não é um membro desta facção.");
                if (requester.role().isLowerThanOrEqualTo(target.role())) {
                    throw new InsufficientPermissionException("Você não tem permissão para rebaixar este membro.");
                }
                if (target.role() == FactionMember.FactionRole.LEADER) {
                    throw new InsufficientPermissionException("O líder da facção não pode ser rebaixado.");
                }

                FactionMember.FactionRole newRole = target.role().getPreviousRole()
                    .orElseThrow(() -> new IllegalStateException("Não é possível rebaixar o membro além do nível de recruta."));

                return updateMemberRole(target, newRole);
            });
    }

    public CompletableFuture<FactionMember> getMember(UUID playerId) {
        return factionMemberCache.getByPlayerId(playerId)
            .thenApply(optMember -> optMember.orElseThrow(() -> new PlayerNotInFactionException("O jogador não é um membro de uma facção.")));
    }

    private CompletableFuture<Faction> removeMemberInternal(Faction faction, User userToRemove) {
        if (faction == null || userToRemove == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Faction and user cannot be null"));
        }

        if (!faction.hasMember(userToRemove.playerId())) {
            return CompletableFuture.failedFuture(new IllegalStateException("User is not a member of this faction"));
        }

        return mongoManager.withTransaction(session -> {
                try {
                    double powerDelta = -userToRemove.power();
                    double maxPowerDelta = -userToRemove.maxPower();

                    CompletableFuture<Void> updatePower = factionRepository.updatePower(
                        faction.factionId(), powerDelta, maxPowerDelta, session);
                    CompletableFuture<Boolean> deleteMember = factionMemberRepository.deleteOneByPlayerId(
                        userToRemove.playerId(), session);

                    return CompletableFuture.allOf(updatePower, deleteMember)
                        .thenCompose(_ -> {
                            faction.removeMember(userToRemove.playerId());
                            return factionRepository.upsert(faction, session);
                        });

                } catch (Exception e) {
                    return CompletableFuture.failedFuture(e);
                }
            })
            .whenComplete((_, throwable) -> {
                if (throwable == null) {
                    factionMemberCache.invalidate(userToRemove.playerId());
                }
            })
            .toCompletableFuture();
    }

    private CompletableFuture<FactionMember> updateMemberRole(FactionMember targetMember, FactionMember.FactionRole newRole) {
        FactionMember updatedMember = targetMember.withRole(newRole);

        return factionMemberRepository.update(updatedMember)
            .thenApply(_ -> {
                factionMemberCache.put(updatedMember);
                return updatedMember;
            });
    }
}
