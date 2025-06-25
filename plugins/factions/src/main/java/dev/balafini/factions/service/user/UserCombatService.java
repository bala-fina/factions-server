package dev.balafini.factions.service.user;

import dev.balafini.factions.config.ConfigManager;
import dev.balafini.factions.model.user.User;
import dev.balafini.factions.service.faction.FactionStatsService;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class UserCombatService {

    private final UserLifecycleService userLifecycleService;
    private final UserStatsService userStatsService;
    private final FactionStatsService factionStatsService;
    private final ConfigManager config;

    public UserCombatService(UserLifecycleService userLifecycleService, UserStatsService userStatsService, FactionStatsService factionStatsService, ConfigManager config) {
        this.userLifecycleService = userLifecycleService;
        this.userStatsService = userStatsService;
        this.factionStatsService = factionStatsService;
        this.config = config;
    }

    public CompletionStage<Void> handlePlayerKill(UUID killerId, String killerName, UUID victimId, String victimName) {
        CompletionStage<User> killerStage = userLifecycleService.getOrCreateUser(killerId, killerName);
        CompletionStage<User> victimStage = userLifecycleService.getOrCreateUser(victimId, victimName);

        return killerStage.thenCombine(victimStage, (killer, victim) -> {
            double powerToGain = calculatePowerGained(killer);
            double powerToLose = calculatePowerLost(victim);
            double killerKdrDelta = calculateKdrDeltaOnKill(killer);
            double victimKdrDelta = calculateKdrDeltaOnDeath(victim);

            CompletionStage<Void> killerPersistence = persistKillerUpdate(killer.playerId(), powerToGain, killerKdrDelta);
            CompletionStage<Void> victimPersistence = persistVictimUpdate(victim.playerId(), powerToLose, victimKdrDelta);

            return CompletableFuture.allOf(killerPersistence.toCompletableFuture(), victimPersistence.toCompletableFuture());
        }).thenCompose(stage -> stage);
    }

    public CompletionStage<Void> handlePveOrSuicideDeath(UUID victimId, String victimName) {
        return userLifecycleService.getOrCreateUser(victimId, victimName)
                .thenCompose(victim -> {
                    double powerToLose = calculatePowerLost(victim);
                    if (powerToLose <= 0) {
                        return CompletableFuture.completedFuture(null);
                    }

                    CompletionStage<Void> userUpdate = userStatsService.applyCombatUpdate(victimId, 0, 0, -powerToLose);
                    CompletionStage<Void> factionUpdate = factionStatsService.syncFactionData(victimId, -powerToLose, 0, 0.0);

                    return CompletableFuture.allOf(userUpdate.toCompletableFuture(), factionUpdate.toCompletableFuture());
                });
    }

    private CompletionStage<Void> persistKillerUpdate(UUID killerId, double powerGained, double kdrDelta) {
        return CompletableFuture.allOf(
                userStatsService.applyCombatUpdate(killerId, 1, 0, powerGained).toCompletableFuture(),
                factionStatsService.syncFactionData(killerId, powerGained, 0, kdrDelta).toCompletableFuture()
        );
    }

    private CompletionStage<Void> persistVictimUpdate(UUID victimId, double powerLost, double kdrDelta) {
        return CompletableFuture.allOf(
                userStatsService.applyCombatUpdate(victimId, 0, 1, -powerLost).toCompletableFuture(),
                factionStatsService.syncFactionData(victimId, -powerLost, 0, kdrDelta).toCompletableFuture()
        );
    }

    private double calculatePowerGained(User killer) {
        if (killer.power() >= killer.maxPower()) return 0;
        return Math.min(config.powerGainedOnKill(), killer.maxPower() - killer.power());
    }

    private double calculatePowerLost(User victim) {
        return Math.min(victim.power(), config.powerLostOnDeath());
    }

    private double calculateKdrDeltaOnKill(User killer) {
        double oldKdr = killer.getKdr();
        double newKdr = (killer.deaths() == 0) ? (killer.kills() + 1) : (double) (killer.kills() + 1) / killer.deaths();
        return newKdr - oldKdr;
    }

    private double calculateKdrDeltaOnDeath(User victim) {
        double oldKdr = victim.getKdr();
        double newKdr = (double) victim.kills() / (victim.deaths() + 1);
        return newKdr - oldKdr;
    }
}
