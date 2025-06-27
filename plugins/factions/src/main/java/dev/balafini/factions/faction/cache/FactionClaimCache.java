package dev.balafini.factions.faction.cache;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.balafini.factions.faction.Faction;
import dev.balafini.factions.faction.FactionClaim;
import dev.balafini.factions.faction.repository.FactionClaimRepository;
import dev.balafini.factions.faction.repository.FactionRepository;
import org.bukkit.Chunk;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

public class FactionClaimCache {
    private final AsyncLoadingCache<UUID, FactionClaim> cacheById;
    private final FactionClaimRepository claimRepository;

    public FactionClaimCache(FactionClaimRepository claimRepository, Executor executor) {
        this.claimRepository = claimRepository;
        this.cacheById = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .executor(executor)
            .buildAsync((id, _) ->
                claimRepository.findById(id)
                    .thenApply(opt -> opt.orElse(null))
                    .toCompletableFuture());
    }

    public CompletionStage<Optional<FactionClaim>> getById(UUID id) {
        if (id == null) return CompletableFuture.completedFuture(Optional.empty());
        return cacheById.get(id).thenApply(Optional::ofNullable);
    }

    public CompletableFuture<Optional<FactionClaim>> getByChunk(Chunk chunk) {
        final var cachedClaim = cacheById.asMap().values().stream()
            .filter(claim -> claim.join().chunkX() == chunk.getX() && claim.join().chunkZ() == chunk.getZ())
            .findFirst();

        return cachedClaim.map(claimCompletableFuture -> claimCompletableFuture.thenApply(Optional::of))
            .orElseGet(() -> claimRepository.findByChunk(chunk).thenApply(optFaction -> {
                optFaction.ifPresent(this::put);
                return optFaction;
            }).toCompletableFuture());
    }

    public void put(FactionClaim claim) {
        cacheById.put(claim.id(), CompletableFuture.completedFuture(claim));
    }

    public void invalidate(FactionClaim claim) {
        if (claim == null) return;
        invalidateById(claim.id());
    }

    public void invalidateById(UUID id) {
        cacheById.synchronous().invalidate(id);
    }
}
