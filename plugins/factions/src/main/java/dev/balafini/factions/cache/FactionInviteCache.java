package dev.balafini.factions.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.balafini.factions.model.FactionInvite;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class FactionInviteCache {

    private final Cache<UUID, FactionInvite> invites;
    private final Map<UUID, UUID> playerIndex = new ConcurrentHashMap<>();

    public FactionInviteCache() {
        this.invites = Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(Duration.ofMinutes(15))
                .build();
    }

    public void put(FactionInvite invite) {
        invites.put(invite.inviteId(), invite);
        playerIndex.put(invite.inviteeId(), invite.inviteId());
    }

    public Optional<FactionInvite> getById(UUID id) {
        return Optional.ofNullable(invites.getIfPresent(id));
    }

    public Optional<FactionInvite> getByPlayer(UUID playerId) {
        UUID inviteId = playerIndex.get(playerId);
        return inviteId != null ? getById(inviteId) : Optional.empty();
    }

    public List<FactionInvite> getByFaction(UUID factionId) {
        return invites.asMap().values().stream()
                .filter(invite -> invite.factionId().equals(factionId))
                .collect(Collectors.toList());
    }

    public void remove(UUID inviteId) {
        getById(inviteId).ifPresent(invite -> {
            invites.invalidate(inviteId);
            playerIndex.remove(invite.inviteeId());
        });
    }

    public boolean hasActiveInvite(UUID playerId) {
        return playerIndex.containsKey(playerId);
    }
}
