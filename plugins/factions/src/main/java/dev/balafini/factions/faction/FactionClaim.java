package dev.balafini.factions.faction;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.bson.types.ObjectId;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.mongojack.Id;

import java.time.Instant;
import java.util.UUID;

public record FactionClaim(
    @Id String objectId,
    UUID id,
    String worldName,
    int chunkX,
    int chunkZ,
    ClaimType claimType,
    UUID factionId,
    UUID claimedBy,
    Instant claimedAt
) {

    public enum ClaimType {
        PLAYER("Normal"),
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

    public static FactionClaim createClaim(
        Chunk chunk,
        ClaimType claimType,
        UUID factionId,
        UUID claimedBy
    ) {
        return new FactionClaim(
            UUID.randomUUID().toString(),
            UUID.randomUUID(),
            chunk.getWorld().getName(),
            chunk.getX(),
            chunk.getZ(),
            claimType,
            factionId,
            claimedBy,
            Instant.now()
        );
    }

    @JsonIgnore
    public World getWorld() {
        return Bukkit.getWorld(worldName);
    }

    @JsonIgnore
    public Chunk getChunk() {
        World world = getWorld();
        if (world == null) {
            throw new IllegalStateException("World not found: " + worldName);
        }

        return world.getChunkAt(chunkX, chunkZ);
    }

}
