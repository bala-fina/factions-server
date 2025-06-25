package dev.balafini.factions.service.user;

import dev.balafini.factions.config.ConfigManager;
import dev.balafini.factions.exception.MaxPowerReachedException;
import dev.balafini.factions.service.faction.FactionStatsService;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class UserPowerService {

    private final UserLifecycleService userLifecycleService;
    private final UserStatsService userStatsService;
    private final FactionStatsService factionStatsService;
    private final ConfigManager config;

    public UserPowerService(UserLifecycleService userLifecycleService, UserStatsService userStatsService, FactionStatsService factionStatsService, ConfigManager config) {
        this.userLifecycleService = userLifecycleService;
        this.userStatsService = userStatsService;
        this.factionStatsService = factionStatsService;
        this.config = config;
    }

    public CompletionStage<Void> increaseMaxPower(UUID playerId, String playerName, double amount) {
        return userLifecycleService.getOrCreateUser(playerId, playerName)
                .thenCompose(user -> {
                    double newMaxPower = Math.min(config.serverMaxPlayerPower(), user.maxPower() + amount);
                    double maxPowerGained = newMaxPower - user.maxPower();

                    if (maxPowerGained <= 0) {
                        throw new MaxPowerReachedException("O jogador já possui o poder máximo.");
                    }

                    CompletionStage<Void> userUpdate = userStatsService.applyMaxPowerUpdate(playerId, newMaxPower);
                    CompletionStage<Void> factionUpdate = factionStatsService.syncFactionData(playerId, 0, maxPowerGained, 0);

                    return CompletableFuture.allOf(userUpdate.toCompletableFuture(), factionUpdate.toCompletableFuture());
                });
    }

    public CompletionStage<Void> regeneratePower(UUID playerId, String playerName) {
        return userLifecycleService.getOrCreateUser(playerId, playerName)
                .thenCompose(user -> {
                    if (user.power() >= user.maxPower()) {
                        return CompletableFuture.completedFuture(null);
                    }

                    double newPower = Math.min(user.maxPower(), user.power() + config.powerGainedPerInterval());
                    double powerGained = newPower - user.power();

                    if (powerGained <= 0) {
                        return CompletableFuture.completedFuture(null);
                    }

                    CompletionStage<Void> userUpdate = userStatsService.applyCombatUpdate(playerId, 0, 0, powerGained);
                    CompletionStage<Void> factionUpdate = factionStatsService.syncFactionData(playerId, powerGained, 0, 0);

                    return CompletableFuture.allOf(userUpdate.toCompletableFuture(), factionUpdate.toCompletableFuture());
                });
    }
}
