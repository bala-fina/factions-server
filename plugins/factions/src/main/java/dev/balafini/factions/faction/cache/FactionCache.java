package dev.balafini.factions.faction.cache;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.balafini.factions.faction.Faction;
import dev.balafini.factions.faction.repository.FactionRepository;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class FactionCache {
    private final AsyncLoadingCache<UUID, Faction> cacheById;
    private final FactionRepository factionRepository;

    public FactionCache(FactionRepository factionRepository, Executor executor) {
        this.factionRepository = factionRepository;
        this.cacheById = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .executor(executor)
            .buildAsync((id, _) ->
                factionRepository.findFactionById(id)
                    .thenApply(opt -> opt.orElse(null))
                    .toCompletableFuture());
    }

    public CompletableFuture<Optional<Faction>> getById(UUID id) {
        if (id == null) return CompletableFuture.completedFuture(Optional.empty());
        return cacheById.get(id).thenApply(Optional::ofNullable);
    }

    public CompletableFuture<Optional<Faction>> getByName(String name) {
        final var cachedFaction = cacheById.asMap().values().stream()
            .filter(faction -> faction.join().name().equalsIgnoreCase(name))
            .findFirst();

        return cachedFaction.map(factionCompletableFuture -> factionCompletableFuture.thenApply(Optional::of))
            .orElseGet(() -> factionRepository.findFactionByName(name).thenApply(optFaction -> {
                optFaction.ifPresent(this::put);
                return optFaction;
            }));
    }

    public CompletableFuture<Optional<Faction>> getByTag(String tag) {
        final var cachedFaction = cacheById.asMap().values().stream()
            .filter(faction -> faction.join().name().equalsIgnoreCase(tag))
            .findFirst();

        return cachedFaction.map(factionCompletableFuture -> factionCompletableFuture.thenApply(Optional::of))
            .orElseGet(() -> factionRepository.findFactionByTag(tag).thenApply(optFaction -> {
                optFaction.ifPresent(this::put);
                return optFaction;
            }));

    }

    public void put(Faction faction) {
        cacheById.put(faction.factionId(), CompletableFuture.completedFuture(faction));
    }

    public void invalidate(Faction faction) {
        if (faction == null) return;

        factionRepository.upsert(faction, null)
            .thenApply(_ -> {
                invalidateById(faction.factionId());
                return null;
            });
    }

    public void invalidateById(UUID id) {
        cacheById.synchronous().invalidate(id);
    }

}
