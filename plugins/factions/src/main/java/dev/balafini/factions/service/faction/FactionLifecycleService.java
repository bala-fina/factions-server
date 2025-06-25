package dev.balafini.factions.service.faction;

import dev.balafini.factions.cache.FactionCache;
import dev.balafini.factions.database.MongoManager;
import dev.balafini.factions.exception.InsufficientPermissionException;
import dev.balafini.factions.exception.InvalidFactionParametersException;
import dev.balafini.factions.exception.PlayerNotInFactionException;
import dev.balafini.factions.model.faction.Faction;
import dev.balafini.factions.model.faction.FactionMember;
import dev.balafini.factions.repository.faction.FactionMemberRepository;
import dev.balafini.factions.repository.faction.FactionRepository;
import dev.balafini.factions.service.user.UserLifecycleService;
import dev.balafini.factions.util.FactionValidator;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class FactionLifecycleService {

    private final FactionCache factionCache;
    private final FactionRepository factionRepository;
    private final FactionMemberRepository factionMemberRepository;
    private final UserLifecycleService userLifecycleService;
    private final FactionQueryService factionQueryService;
    private final FactionValidator factionValidator;
    private final MongoManager mongoManager;

    public FactionLifecycleService(FactionCache factionCache, FactionRepository factionRepository, FactionMemberRepository factionMemberRepository, UserLifecycleService userLifecycleService, FactionQueryService factionQueryService, FactionValidator factionValidator, MongoManager mongoManager) {
        this.factionCache = factionCache;
        this.factionRepository = factionRepository;
        this.factionMemberRepository = factionMemberRepository;
        this.userLifecycleService = userLifecycleService;
        this.factionQueryService = factionQueryService;
        this.factionValidator = factionValidator;
        this.mongoManager = mongoManager;
    }

    public CompletionStage<Faction> createFaction(String tag, String name, UUID leaderId, String leaderName) {
        try {
            FactionValidator.validateFactionParameters(name, tag);
        } catch (InvalidFactionParametersException e) {
            return CompletableFuture.failedFuture(e);
        }

        return factionValidator.validateFactionCreation(name, tag, leaderId)
                .thenCompose(_ -> userLifecycleService.getOrCreateUser(leaderId, leaderName))
                .thenCompose(leaderUser -> mongoManager.withTransaction(session -> {
                    Faction newFaction = Faction.create(name, tag, leaderUser.playerId(), leaderUser.power(), leaderUser.maxPower(), leaderUser.getKdr());
                    FactionMember factionLeader = newFaction.getLeader();

                    CompletionStage<Void> saveFactionStage = factionRepository.upsert(newFaction, session);
                    CompletionStage<Void> saveLeaderStage = factionMemberRepository.insert(factionLeader, session);

                    return CompletableFuture.allOf(saveFactionStage.toCompletableFuture(), saveLeaderStage.toCompletableFuture())
                            .thenApply(_ -> newFaction);
                })).thenApply(newFaction -> {
                    factionCache.put(newFaction);
                    return newFaction;
                });
    }

    public CompletionStage<Void> disbandFaction(UUID requesterId) {

        return factionQueryService.findFactionByPlayer(requesterId)
                .thenCompose(optFaction -> {
                    if (optFaction.isEmpty()) {
                        throw new PlayerNotInFactionException("Você não está em uma facção para desfazer.");
                    }

                    Faction faction = optFaction.get();
                    if (!faction.getLeader().playerId().equals(requesterId)) {
                        throw new InsufficientPermissionException("Apenas o líder da facção pode excluir a facção.");
                    }

                    factionCache.invalidate(faction);

                    return mongoManager.withTransaction(session -> {
                        CompletionStage<Boolean> deleteFactionStage = factionRepository.deleteByFactionId(faction.factionId(), session);
                        CompletionStage<Long> deleteMembersStage = factionMemberRepository.deleteByFactionId(faction.factionId(), session);

                        return CompletableFuture.allOf(deleteFactionStage.toCompletableFuture(), deleteMembersStage.toCompletableFuture());
                    });
                });
    }
}
