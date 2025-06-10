package dev.balafini.factions.model;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record Faction(
        UUID id,
        String name,
        String tag,
        Instant createdAt,
        Set<FactionUser> members
) {

    public static final String FACTION_NAME_PATTERN = "^[a-zA-Z0-9]{6,12}$";
    public static final String FACTION_TAG_PATTERN = "^[a-zA-Z0-9]{3,4}$";

    public FactionUser getLeader() {
        return members.stream()
                .filter(user -> user.role() == FactionUser.FactionRole.LEADER)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Faction must have a leader."));
    }

    public boolean isMember(UUID playerId) {
        return members.stream().anyMatch(user -> user.playerId().equals(playerId));
    }

    public FactionUser getMember(UUID playerId) {
        return members.stream()
                .filter(user -> user.playerId().equals(playerId))
                .findFirst()
                .orElse(null);
    }
}
