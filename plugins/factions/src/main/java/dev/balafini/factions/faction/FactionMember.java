package dev.balafini.factions.faction;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public record FactionMember(
    UUID playerId,
    UUID factionId,
    FactionRole role,
    Instant joinedAt
) {

    public enum FactionRole {
        RECRUIT,
        MEMBER,
        OFFICER,
        LEADER;

        public boolean isHigherThan(FactionRole other) {
            return this.ordinal() > other.ordinal();
        }

        public boolean isLowerThanOrEqualTo(FactionRole other) {
            return this.ordinal() <= other.ordinal();
        }

        public Optional<FactionRole> getNextRole() {
            if (this == LEADER) return Optional.empty();
            return Optional.of(values()[this.ordinal() + 1]);
        }

        public Optional<FactionRole> getPreviousRole() {
            if (this == RECRUIT) return Optional.empty();
            return Optional.of(values()[this.ordinal() - 1]);
        }

        public boolean canInvite() {
            return this.ordinal() >= OFFICER.ordinal();
        }
    }

    public static FactionMember create(UUID playerId, FactionRole role, UUID factionId) {
        return new FactionMember(playerId, factionId, role, Instant.now());
    }

    public FactionMember withRole(FactionRole newRole) {
        return new FactionMember(playerId, factionId, newRole, joinedAt);
    }
}
