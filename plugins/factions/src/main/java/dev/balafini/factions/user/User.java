package dev.balafini.factions.user;

import org.mongojack.Id;
import org.mongojack.ObjectId;

import java.time.Instant;
import java.util.UUID;

public record User(
        UUID playerId,
        String displayName,
        int kills,
        int deaths,
        double power,
        double maxPower,
        Instant firstJoin,
        Instant lastSeen
) {

    public static User createUser(UUID playerId, String displayName, double initialPower, double initialMaxPower) {
        return new User(
                playerId,
                displayName,
                0,
                0,
                initialPower,
                initialMaxPower,
                Instant.now(),
                Instant.now()
        );
    }

    public User withDisplayName(String newDisplayName) {
        return new User(playerId, newDisplayName, kills, deaths, power, maxPower, firstJoin, lastSeen);
    }

    public User withPower(double newPower) {
        return new User(playerId, this.displayName, kills, deaths, newPower, maxPower, firstJoin, lastSeen);
    }

    public User withMaxPower(double newMaxPower) {
        return new User(playerId, this.displayName, kills, deaths, power, newMaxPower, firstJoin, lastSeen);
    }

    public double getKdr() {
        if (this.deaths == 0) {
            return this.kills;
        }
        return (double) this.kills / this.deaths;
    }
}
