package dev.balafini.factions.service.user;

import dev.balafini.factions.cache.FactionCache;
import dev.balafini.factions.cache.UserCache;
import dev.balafini.factions.config.ConfigManager;
import dev.balafini.factions.model.user.User;
import dev.balafini.factions.repository.faction.FactionMemberRepository;
import dev.balafini.factions.repository.faction.FactionRepository;
import dev.balafini.factions.repository.user.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class UserService {

    private record UserPair(User killer, User victim) {
    }

    private final UserCache userCache;
    private final UserRepository userRepository;
    private final FactionRepository factionRepository;
    private final FactionMemberRepository memberRepository;
    private final FactionCache factionCache;
    private final ConfigManager config;

    public UserService(UserRepository userRepository, UserCache userCache, FactionRepository factionRepository, FactionMemberRepository memberRepository, FactionCache factionCache, ConfigManager config) {
        this.userRepository = userRepository;
        this.userCache = userCache;
        this.factionRepository = factionRepository;
        this.memberRepository = memberRepository;
        this.factionCache = factionCache;
        this.config = config;
    }

    public CompletionStage<User> createUser(UUID playerId) {
        return userRepository.findByPlayerId(playerId).thenCompose(optUser -> {
            if (optUser.isPresent()) {
                return CompletableFuture.completedFuture(optUser.get());
            }
            User newUser = User.createUser(playerId, config.initialPlayerPower(), config.initialMaxPlayerPower());
            return userRepository.save(newUser).thenApply(_ -> {
                userCache.put(newUser);
                return newUser;
            });
        });
    }

    public CompletionStage<Optional<User>> findUser(UUID playerId) {
        return userCache.getById(playerId);
    }

    public CompletionStage<Void> increaseMaxPower(UUID playerId, double amount) {
        return findUser(playerId).thenCompose(optUser -> {
            if (optUser.isEmpty()) {
                return CompletableFuture.completedFuture(null);
            }
            User user = optUser.get();

            double newMaxPower = Math.min(config.serverMaxPlayerPower(), user.maxPower() + amount);
            double maxPowerGained = newMaxPower - user.maxPower();

            if (maxPowerGained <= 0) {
                return CompletableFuture.failedFuture(new IllegalStateException("O jogador já atingiu o poder máximo permitido."));
            }

            User updatedUser = user.withMaxPower(newMaxPower);
            return syncFactionData(playerId, 0, maxPowerGained, 0)
                    .thenCompose(v -> userRepository.save(updatedUser));
        });
    }

    public CompletionStage<Void> regeneratePower(UUID playerId) {
        return findUser(playerId).thenCompose(optUser -> {
            if (optUser.isEmpty()) {
                return CompletableFuture.completedFuture(null);
            }
            User user = optUser.get();

            if (user.power() >= user.maxPower()) {
                return CompletableFuture.completedFuture(null); // Já está no máximo
            }

            double newPower = Math.min(user.maxPower(), user.power() + config.powerGainedPerInterval());
            double powerGained = newPower - user.power();

            if (powerGained <= 0) return CompletableFuture.completedFuture(null);

            User updatedUser = user.withPower(newPower);
            return syncFactionData(playerId, powerGained, 0, 0)
                    .thenCompose(_ -> userRepository.save(updatedUser));
        });
    }

    public CompletionStage<Void> updateUserLastSeen(UUID playerId) {
        userCache.invalidate(playerId);
        return userRepository.updateLastSeen(playerId);
    }

    public CompletionStage<Void> handlePveOrSuicideDeath(UUID victimId) {
        return findUser(victimId).thenCompose(optVictim -> optVictim.map(victim -> {
            double powerToLose = calculatePowerLost(victim);
            double kdrDelta = calculateKdrDeltaOnDeath(victim);

            List<CompletionStage<?>> updates = new ArrayList<>();
            updates.add(userRepository.incrementDeaths(victimId));

            if (powerToLose > 0) {
                updates.add(userRepository.updatePower(victimId, -powerToLose));
            }
            updates.add(syncFactionData(victimId, -powerToLose, 0, kdrDelta));

            userCache.invalidate(victimId);
            return CompletableFuture.allOf(toCompletableFutureArray(updates));
        }).orElse(CompletableFuture.completedFuture(null)));
    }


    public CompletionStage<Void> handlePlayerKill(UUID killerId, UUID victimId) {
        return findUser(killerId).thenCombine(findUser(victimId), (optKiller, optVictim) ->
                optKiller.flatMap(killer -> optVictim.map(victim -> new UserPair(killer, victim)))
        ).thenCompose(optPair -> optPair.map(pair -> {
            User killer = pair.killer();
            User victim = pair.victim();

            double powerToGain = calculatePowerGained(killer);
            double powerToLose = calculatePowerLost(victim);
            double killerKdrDelta = calculateKdrDeltaOnKill(killer);
            double victimKdrDelta = calculateKdrDeltaOnDeath(victim);

            List<CompletionStage<?>> databaseOperations = new ArrayList<>();

            databaseOperations.add(userRepository.incrementKills(killerId));
            if (powerToGain > 0) {
                databaseOperations.add(userRepository.updatePower(killerId, powerToGain));
            }
            databaseOperations.add(syncFactionData(killerId, powerToGain, 0, killerKdrDelta));

            databaseOperations.add(userRepository.incrementDeaths(victimId));
            if (powerToLose > 0) {
                databaseOperations.add(userRepository.updatePower(victimId, -powerToLose));
            }
            databaseOperations.add(syncFactionData(victimId, -powerToLose, 0, victimKdrDelta));

            userCache.invalidate(killerId);
            userCache.invalidate(victimId);

            return CompletableFuture.allOf(toCompletableFutureArray(databaseOperations));
        }).orElse(CompletableFuture.completedFuture(null)));
    }

    public CompletionStage<List<User>> getTopKdrPlayers(int limit) {
        return userRepository.getTopKdrPlayers(limit);
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

    private CompletionStage<Void> syncFactionData(UUID playerId, double powerDelta, double maxPowerDelta, double kdrDelta) {
        if (powerDelta == 0 && maxPowerDelta == 0 && kdrDelta == 0) {
            return CompletableFuture.completedFuture(null);
        }
        return memberRepository.findByPlayerId(playerId).thenCompose(optMember -> {
            if (optMember.isPresent()) {
                UUID factionId = optMember.get().factionId();
                List<CompletionStage<?>> updates = new ArrayList<>();
                if (powerDelta != 0 || maxPowerDelta != 0) {
                    updates.add(factionRepository.updatePower(factionId, powerDelta, maxPowerDelta));
                }
                if (kdrDelta != 0) {
                    updates.add(factionRepository.updateKdr(factionId, kdrDelta));
                }
                return CompletableFuture.allOf(toCompletableFutureArray(updates))
                        .thenRun(() -> factionCache.invalidate(factionId));
            }
            return CompletableFuture.completedFuture(null);
        });
    }

    private CompletableFuture<?>[] toCompletableFutureArray(List<CompletionStage<?>> stages) {
        return stages.stream()
                .map(CompletionStage::toCompletableFuture)
                .toArray(CompletableFuture[]::new);
    }
}
