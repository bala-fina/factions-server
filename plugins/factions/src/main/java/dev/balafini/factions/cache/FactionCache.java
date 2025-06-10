package dev.balafini.factions.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.balafini.factions.model.Faction;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FactionCache {

    private final Cache<UUID, Faction> factions;
    private final Map<String, UUID> nameIndex = new ConcurrentHashMap<>();
    private final Map<String, UUID> tagIndex = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> playerIndex = new ConcurrentHashMap<>();

    public FactionCache() {
        this.factions = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(Duration.ofMinutes(30))
                .removalListener((key, value, cause) -> {
                    if (key != null && value != null) {
                        Faction faction = (Faction) value;
                        nameIndex.remove(faction.name().toLowerCase());
                        tagIndex.remove(faction.tag().toLowerCase());
                        faction.members().forEach(member ->
                                playerIndex.remove(member.playerId()));
                    }
                })
                .build();
    }

    public void put(Faction faction) {
        factions.put(faction.id(), faction);
        nameIndex.put(faction.name().toLowerCase(), faction.id());
        tagIndex.put(faction.tag().toLowerCase(), faction.id());

        faction.members().forEach(member ->
                playerIndex.put(member.playerId(), faction.id()));
    }

    public Optional<Faction> getById(UUID id) {
        return Optional.ofNullable(factions.getIfPresent(id));
    }

    public Optional<Faction> getByName(String name) {
        UUID id = nameIndex.get(name.toLowerCase());
        return id != null ? getById(id) : Optional.empty();
    }

    public Optional<Faction> getByTag(String tag) {
        UUID id = tagIndex.get(tag.toLowerCase());
        return id != null ? getById(id) : Optional.empty();
    }

    public Optional<Faction> getByPlayer(UUID playerId) {
        UUID id = playerIndex.get(playerId);
        return id != null ? getById(id) : Optional.empty();
    }

    public void remove(UUID id) {
        getById(id).ifPresent(faction -> {
            factions.invalidate(id);
            nameIndex.remove(faction.name().toLowerCase());
            tagIndex.remove(faction.tag().toLowerCase());
            faction.members().forEach(member ->
                    playerIndex.remove(member.playerId()));
        });
    }

    public boolean existsByName(String name) {
        return nameIndex.containsKey(name.toLowerCase());
    }

    public boolean existsByTag(String tag) {
        return tagIndex.containsKey(tag.toLowerCase());
    }
}
