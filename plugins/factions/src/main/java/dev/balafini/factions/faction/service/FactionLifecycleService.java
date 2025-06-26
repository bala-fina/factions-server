package dev.balafini.factions.faction.service;

import dev.balafini.factions.faction.FactionClaim;
import dev.balafini.factions.faction.cache.FactionCache;
import dev.balafini.factions.database.MongoManager;
import dev.balafini.factions.faction.cache.FactionClaimCache;
import dev.balafini.factions.faction.exception.InsufficientPermissionException;
import dev.balafini.factions.faction.exception.InvalidFactionClaimException;
import dev.balafini.factions.faction.exception.InvalidFactionParametersException;
import dev.balafini.factions.faction.exception.PlayerNotInFactionException;
import dev.balafini.factions.faction.Faction;
import dev.balafini.factions.faction.FactionMember;
import dev.balafini.factions.faction.repository.FactionClaimRepository;
import dev.balafini.factions.faction.repository.FactionMemberRepository;
import dev.balafini.factions.faction.repository.FactionRepository;
import dev.balafini.factions.faction.validator.FactionClaimValidator;
import dev.balafini.factions.user.service.UserLifecycleService;
import dev.balafini.factions.faction.validator.FactionValidator;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

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

    public FactionLifecycleService(
        FactionCache factionCache,
        FactionRepository factionRepository,
        FactionMemberRepository factionMemberRepository,
        UserLifecycleService userLifecycleService,
        FactionQueryService factionQueryService,
        FactionValidator factionValidator,
        MongoManager mongoManager
    ) {
        this.factionCache = factionCache;

        this.factionRepository = factionRepository;
        this.factionMemberRepository = factionMemberRepository;

        this.userLifecycleService = userLifecycleService;
        this.factionQueryService = factionQueryService;

        this.factionValidator = factionValidator;

        this.mongoManager = mongoManager;
    }

    public CompletionStage<Faction> createFaction(String tag, String name, Player player) {
        try {
            FactionValidator.validateFactionParameters(name, tag);
        } catch (InvalidFactionParametersException e) {
            return CompletableFuture.failedFuture(e);
        }

        return factionValidator.validateFactionCreation(name, tag, player.getUniqueId())
            .thenCompose(_ -> userLifecycleService.getOrCreateUser(player.getUniqueId(), player.getName()))
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
