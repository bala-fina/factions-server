package dev.balafini.factions.model.faction;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import java.time.Instant;
import java.util.UUID;

public record FactionMember(
        @Id @ObjectId @JsonProperty("_id") String id,
        UUID playerId,
        UUID factionId,
        FactionRole role,
        Instant joinedAt
) {

    public enum FactionRole {
        LEADER(1), OFFICER(2), MEMBER(3), RECRUIT(4);
        private final int level;

        FactionRole(int level) {
            this.level = level;
        }

        public boolean isHigherThan(FactionRole other) {
            return this.level < other.level;
        }

        public FactionRole getNextHigherRole() {
            return switch (this) {
                case RECRUIT -> MEMBER;
                case MEMBER -> OFFICER;
                default -> null;
            };
        }

        public FactionRole getNextLowerRole() {
            return switch (this) {
                case OFFICER -> MEMBER;
                case MEMBER -> RECRUIT;
                default -> null;
            };
        }
    }

    public static FactionMember create(UUID playerId, FactionRole role, UUID factionId) {
        return new FactionMember(null, playerId, factionId, role, Instant.now());
    }

    public FactionMember withRole(FactionRole newRole) {
        return new FactionMember(this.id, this.playerId, this.factionId, newRole, this.joinedAt);
    }
}
