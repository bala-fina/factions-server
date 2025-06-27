package dev.balafini.factions.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownUtil {
    private final Cache<UUID, Map<String, Long>> cooldowns = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofHours(1))
            .build();

    public void setCooldown(UUID playerId, String cooldownId, Duration duration) {
        Map<String, Long> playerCooldowns = cooldowns.get(playerId, _ -> new ConcurrentHashMap<>());
        if (playerCooldowns != null) {
            playerCooldowns.put(cooldownId.toLowerCase(), System.currentTimeMillis() + duration.toMillis());
        }
    }

    public Optional<Duration> getTimeLeft(UUID playerId, String cooldownId) {
        Map<String, Long> playerCooldowns = cooldowns.getIfPresent(playerId);
        if (playerCooldowns == null) {
            return Optional.empty();
        }

        Long expiration = playerCooldowns.get(cooldownId.toLowerCase());
        if (expiration == null || System.currentTimeMillis() > expiration) {
            return Optional.empty();
        }
        return Optional.of(Duration.ofMillis(expiration - System.currentTimeMillis()));
    }
}
