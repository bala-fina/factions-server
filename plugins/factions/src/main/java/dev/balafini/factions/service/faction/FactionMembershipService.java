package dev.balafini.factions.service.faction;

import dev.balafini.factions.cache.FactionCache;
import dev.balafini.factions.config.ConfigManager;
import dev.balafini.factions.database.MongoManager;
import dev.balafini.factions.exception.*;
import dev.balafini.factions.model.faction.Faction;
import dev.balafini.factions.model.faction.FactionMember;
import dev.balafini.factions.model.user.User;
import dev.balafini.factions.repository.faction.FactionMemberRepository;
import dev.balafini.factions.repository.faction.FactionRepository;
import dev.balafini.factions.service.user.UserLifecycleService;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class FactionMembershipService {

    private final UserLifecycleService userLifecycleService;
    private final FactionQueryService factionQueryService;
    private final FactionMemberRepository factionMemberRepository;
    private final FactionRepository factionRepository;
    private final MongoManager mongoManager;
    private final FactionCache factionCache;
    private final ConfigManager config;

    public FactionMembershipService(UserLifecycleService userLifecycleService, FactionQueryService factionQueryService, FactionMemberRepository factionMemberRepository, FactionRepository factionRepository, MongoManager mongoManager, FactionCache factionCache, ConfigManager config) {
        this.userLifecycleService = userLifecycleService;
        this.factionQueryService = factionQueryService;
        this.factionMemberRepository = factionMemberRepository;
        this.factionRepository = factionRepository;
        this.mongoManager = mongoManager;
        this.factionCache = factionCache;
        this.config = config;
    }

    public CompletionStage<Void> addMember(UUID factionId, UUID newMemberId, String newMemberDisplayName) {
        return userLifecycleService.getOrCreateUser(newMemberId, newMemberDisplayName)
                .thenCompose(user -> factionQueryService.findFactionById(factionId)
                        .thenCompose(optFaction -> {
                            if (optFaction.isEmpty()) {
                                throw new FactionNotFoundException("Facção não encontrada.");
                            }
                            Faction faction = optFaction.get();

                            if (faction.members().size() >= config.maxFactionSize()) {
                                throw new FactionFullException("Esta facção já está cheia.");
                            }
                            if (faction.getMember(newMemberId) != null) {
                                throw new PlayerAlreadyInFactionException("O jogador já é um membro desta facção.");
                            }
                            return mongoManager.withTransaction(session -> {
                                FactionMember newMember = FactionMember.create(newMemberId, FactionMember.FactionRole.RECRUIT, factionId);

                                CompletionStage<Void> saveMemberStage = factionMemberRepository.insert(newMember, session);
                                CompletionStage<Void> updatePowerStage = factionRepository.updatePower(factionId, user.power(), user.maxPower(), session);

                                return CompletableFuture.allOf(saveMemberStage.toCompletableFuture(), updatePowerStage.toCompletableFuture());
                            });
                        }))
                .thenRun(() -> factionCache.invalidateById(factionId));
    }

    public CompletionStage<Void> leaveFaction(UUID requesterId) {
        return factionQueryService.findFactionByPlayer(requesterId)
                .thenCompose(optFaction -> {
                    if (optFaction.isEmpty()) {
                        throw new PlayerNotInFactionException("Você não está em uma facção.");
                    }
                    Faction faction = optFaction.get();
                    FactionMember requester = faction.getMember(requesterId);

                    if (requester.role() == FactionMember.FactionRole.LEADER) {
                        throw new InsufficientPermissionException("O líder não pode sair da facção. Use o comando /f disband para dissolver a facção.");
                    }

                    return userLifecycleService.getOrCreateUser(requesterId, "Unknown")
                            .thenCompose(user -> removeMemberInternal(faction, user));
                });
    }

    public CompletionStage<Void> kickMember(UUID requesterId, UUID targetId) {
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
                            .thenCompose(user -> removeMemberInternal(faction, user));
                });
    }

    public CompletionStage<Void> promoteMember(UUID requesterId, UUID targetId) {
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

    public CompletionStage<Void> demoteMember(UUID requesterId, UUID targetId) {
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

    private CompletionStage<Void> removeMemberInternal(Faction faction, User userToRemove) {
        return mongoManager.withTransaction(session -> {
            double powerDelta = -userToRemove.power();
            double maxPowerDelta = -userToRemove.maxPower();

            CompletionStage<Void> updatePowerStage = factionRepository.updatePower(faction.factionId(), powerDelta, maxPowerDelta, session);
            CompletionStage<Boolean> deleteMemberStage = factionMemberRepository.deleteByPlayerId(userToRemove.playerId(), session);

            return CompletableFuture.allOf(updatePowerStage.toCompletableFuture(), deleteMemberStage.toCompletableFuture());
        }).thenRun(() -> factionCache.invalidateById(faction.factionId()));
    }

    private CompletionStage<Void> updateMemberRole(FactionMember targetMember, FactionMember.FactionRole newRole) {
        FactionMember updatedMember = targetMember.withRole(newRole);
        return factionMemberRepository.update(updatedMember).thenRun(() -> factionCache.invalidateById(targetMember.factionId()));
    }
}
