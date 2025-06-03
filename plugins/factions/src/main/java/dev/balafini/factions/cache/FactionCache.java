package dev.balafini.factions.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.balafini.factions.model.Faction;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class FactionCache {

    private final Cache<UUID, Faction> factionCache;
    private final ConcurrentMap<String, UUID> nameToIdMap;
    private final ConcurrentMap<String, UUID> tagToIdMap;
    private final ConcurrentMap<UUID, UUID> playerToFactionMap;

    public FactionCache() {
        this.factionCache = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(Duration.ofMinutes(30))
                .build();

        this.nameToIdMap = new ConcurrentHashMap<>();
        this.tagToIdMap = new ConcurrentHashMap<>();
        this.playerToFactionMap = new ConcurrentHashMap<>();
    }

    public void put(Faction faction) {
        factionCache.put(faction.id(), faction);
        nameToIdMap.put(faction.name(), faction.id());
        tagToIdMap.put(faction.tag(), faction.id());

        faction.members().forEach(member ->
                playerToFactionMap.put(member.playerId(), faction.id())
        );
    }

    public Optional<Faction> getById(UUID factionId) {
        return Optional.ofNullable(factionCache.getIfPresent(factionId));
    }

    public Optional<Faction> getByName(String name) {
        UUID factionId = nameToIdMap.get(name.toLowerCase());
        return factionId != null ? getById(factionId) : Optional.empty();
    }

    public Optional<Faction> getByTag(String tag) {
        UUID factionId = tagToIdMap.get(tag.toLowerCase());
        return factionId != null ? getById(factionId) : Optional.empty();
    }

    public Optional<Faction> getByPlayer(UUID playerId) {
        UUID factionId = playerToFactionMap.get(playerId);
        return factionId != null ? getById(factionId) : Optional.empty();
    }

    public void remove(UUID factionId) {
        Faction faction = factionCache.getIfPresent(factionId);
        if (faction != null) {
            factionCache.invalidate(factionId);
            nameToIdMap.remove(faction.name().toLowerCase());
            tagToIdMap.remove(faction.tag().toLowerCase());

            faction.members().forEach(member ->
                    playerToFactionMap.remove(member.playerId())
            );
        }
    }

    public boolean existsByName(String name) {
        return nameToIdMap.containsKey(name.toLowerCase());
    }

    public boolean existsByTag(String tag) {
        return tagToIdMap.containsKey(tag.toLowerCase());
    }

    public void clear() {
        factionCache.invalidateAll();
        nameToIdMap.clear();
        tagToIdMap.clear();
        playerToFactionMap.clear();
    }
}
