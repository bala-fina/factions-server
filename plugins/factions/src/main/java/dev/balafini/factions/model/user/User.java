package dev.balafini.factions.model.user;

import org.mongojack.Id;
import org.mongojack.ObjectId;

import java.time.Instant;
import java.util.UUID;

public record User(
        @Id @ObjectId String id,
        UUID playerId,
        int kills,
        int deaths,
        double power,
        double maxPower,
        Instant firstJoin,
        Instant lastSeen
) {

    public static User createUser(UUID playerId, double initialPower, double initialMaxPower) {
        return new User(
                null,
                playerId,
                0,
                0,
                initialPower,
                initialMaxPower,
                Instant.now(),
                Instant.now()
        );
    }

    public User withPower(double newPower) {
        return new User(id, playerId, kills, deaths, newPower, maxPower, firstJoin, lastSeen);
    }

    public User withMaxPower(double newMaxPower) {
        return new User(id, playerId, kills, deaths, power, newMaxPower, firstJoin, lastSeen);
    }

    public double getKdr() {
        if (this.deaths == 0) {
            return this.kills;
        }
        return (double) this.kills / this.deaths;
    }
}
