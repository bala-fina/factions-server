package dev.balafini.factions.service.user;

import dev.balafini.factions.cache.UserCache;
import dev.balafini.factions.repository.user.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class UserStatsService {

    private final UserRepository userRepository;
    private final UserCache userCache;

    public UserStatsService(UserRepository userRepository, UserCache userCache) {
        this.userRepository = userRepository;
        this.userCache = userCache;
    }

    public CompletionStage<Void> applyCombatUpdate(UUID userId, int killsDelta, int deathsDelta, double powerDelta) {
        List<CompletionStage<?>> updates = new ArrayList<>();
        if (killsDelta > 0) updates.add(userRepository.incrementKills(userId));
        if (deathsDelta > 0) updates.add(userRepository.incrementDeaths(userId));
        if (powerDelta != 0) updates.add(userRepository.updatePower(userId, powerDelta));

        if (updates.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<?>[] futures = updates.stream()
                .map(CompletionStage::toCompletableFuture)
                .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(futures)
                .thenRun(() -> userCache.invalidateById(userId));
    }

    public CompletionStage<Void> applyMaxPowerUpdate(UUID userId, double newMaxPower) {
        return userRepository.setMaxPower(userId, newMaxPower)
                .thenRun(() -> userCache.invalidateById(userId));
    }
}
