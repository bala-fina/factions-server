package dev.balafini.factions.cache;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.balafini.factions.model.faction.Faction;
import dev.balafini.factions.repository.faction.FactionRepository;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

public class FactionCache {
    private final AsyncLoadingCache<UUID, Faction> cacheById;
    private final Cache<String, UUID> nameToIdCache;
    private final Cache<String, UUID> tagToIdCache;
    private final FactionRepository factionRepository;

    public FactionCache(FactionRepository factionRepository, Executor executor) {
        this.factionRepository = factionRepository;
        this.cacheById = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(Duration.ofMinutes(30))
                .executor(executor)
                .buildAsync((id, exec) ->
                        factionRepository.findFullFactionById(id)
                                .thenApply(opt -> opt.orElse(null))
                                .toCompletableFuture());

        this.nameToIdCache = Caffeine.newBuilder().maximumSize(2000).expireAfterWrite(Duration.ofMinutes(30)).build();
        this.tagToIdCache = Caffeine.newBuilder().maximumSize(2000).expireAfterWrite(Duration.ofMinutes(30)).build();
    }

    public CompletionStage<Optional<Faction>> getById(UUID id) {
        if (id == null) return CompletableFuture.completedFuture(Optional.empty());
        return cacheById.get(id).thenApply(Optional::ofNullable);
    }

    public CompletionStage<Optional<Faction>> getByName(String name) {
        UUID factionId = nameToIdCache.getIfPresent(name);
        if (factionId != null) {
            return getById(factionId);
        }
        return factionRepository.findFullFactionByName(name).thenApply(optFaction -> {
            optFaction.ifPresent(this::put);
            return optFaction;
        });
    }

    public CompletionStage<Optional<Faction>> getByTag(String tag) {
        UUID factionId = tagToIdCache.getIfPresent(tag);
        if (factionId != null) {
            return getById(factionId);
        }
        return factionRepository.findFullFactionByTag(tag).thenApply(optFaction -> {
            optFaction.ifPresent(this::put);
            return optFaction;
        });
    }

    public void put(Faction faction) {
        cacheById.put(faction.factionId(), CompletableFuture.completedFuture(faction));
        nameToIdCache.put(faction.name(), faction.factionId());
        tagToIdCache.put(faction.tag(), faction.factionId());
    }

    public void invalidate(Faction faction) {
        if (faction == null) return;
        invalidateById(faction.factionId());
        nameToIdCache.invalidate(faction.name());
        tagToIdCache.invalidate(faction.tag());
    }

    public void invalidateById(UUID id) {
        cacheById.synchronous().invalidate(id);
    }
}
