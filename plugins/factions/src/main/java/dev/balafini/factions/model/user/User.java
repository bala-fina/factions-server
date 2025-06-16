package dev.balafini.factions.model.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import java.time.Instant;
import java.util.UUID;

public record User(
        @Id
        @ObjectId
        String id,

        @JsonProperty("uuid")
        UUID playerId,

        @JsonProperty("kills")
        int kills,

        @JsonProperty("deaths")
        int deaths,

        @JsonProperty("power")
        int power,

        @JsonProperty("maxPower")
        int maxPower,

        @JsonProperty("firstJoinAt")
        Instant firstJoinAt,

        @JsonProperty("lastSeen")
        Instant lastSeen
) {

    public static User createUser(UUID playerId) {
        return new User(
                null,
                playerId,
                0,
                0,
                0,
                20,
                Instant.now(),
                Instant.now()
        );
    }
}
