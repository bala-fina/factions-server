package dev.balafini.factions.model;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record Faction(
        UUID id,
        String name,
        String tag,
        Instant createdAt,
        Set<FactionUser> members
) {

    private static final String FACTION_NAME_PATTERN = "^[a-zA-Z0-9]{6,12}$";
    private static final String FACTION_TAG_PATTERN = "^[a-zA-Z0-9]{3,4}$";

    public Faction {
        if (!name.matches(FACTION_NAME_PATTERN)) {
            throw new IllegalArgumentException("Faction name must be between 6 and 12 alphanumeric characters.");
        }
        if (!tag.matches(FACTION_TAG_PATTERN)) {
            throw new IllegalArgumentException("Faction tag must be between 3 and 4 alphanumeric characters.");
        }

        if (members.isEmpty()) {
            members = Set.of();
        }
    }

    public FactionUser getLeader() {
        return members.stream()
                .filter(user -> user.role() == FactionUser.FactionRole.LEADER)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Faction must have a leader."));
    }

    public Set<FactionUser> getOfficers() {
        return members.stream()
                .filter(user -> user.role() == FactionUser.FactionRole.OFFICER)
                .collect(Collectors.toSet());
    }

    public boolean isMember(UUID playerId) {
        return members.stream()
                .anyMatch(user -> user.playerId().equals(playerId));
    }
}
