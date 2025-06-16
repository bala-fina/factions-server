package dev.balafini.factions.model.faction;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import java.time.Instant;

public record FactionClaim(
        @Id
        @ObjectId
        String id,

        @JsonProperty("worldName")
        String worldName,

        @JsonProperty("chunkX")
        int chunkX,

        @JsonProperty("chunkZ")
        int chunkZ,

        @JsonProperty("claimType")
        ClaimType claimType,

        @JsonProperty("factionTag")
        String factionTag,

        @JsonProperty("claimedAt")
        Instant claimedAt
) {
    public enum ClaimType {
        NORMAL("Normal"),
        SAFEZONE("Safezone"),
        WARZONE("Warzone");

        private final String name;

        ClaimType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public static FactionClaim createClaim(String worldName, int chunkX, int chunkZ, ClaimType claimType, String factionTag) {
        return new FactionClaim(
                null,
                worldName,
                chunkX,
                chunkZ,
                claimType,
                factionTag,
                Instant.now()
        );
    }
}
