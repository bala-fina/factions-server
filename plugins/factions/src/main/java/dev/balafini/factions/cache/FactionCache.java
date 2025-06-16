package dev.balafini.factions.cache;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.balafini.factions.model.faction.Faction;
import dev.balafini.factions.repository.faction.FactionMemberRepository;
import dev.balafini.factions.repository.faction.FactionRepository;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

public class FactionCache {

    private final AsyncLoadingCache<UUID, Faction> cache;

    public FactionCache(FactionRepository factionRepository, FactionMemberRepository memberRepository, Executor executor) {
        this.cache = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(Duration.ofMinutes(30))
                .executor(executor)
                .buildAsync((id, exec) -> fetchAndHydrate(id, factionRepository, memberRepository).toCompletableFuture());
    }

    public CompletionStage<Optional<Faction>> getById(UUID id) {
        return cache.get(id).thenApply(Optional::ofNullable);
    }

    public void put(Faction faction) {
        cache.put(faction.factionId(), CompletableFuture.completedFuture(faction));
    }

    public void invalidate(UUID id) {
        cache.synchronous().invalidate(id);
    }

    private static CompletionStage<Faction> fetchAndHydrate(UUID factionId, FactionRepository factionRepo, FactionMemberRepository memberRepo) {
        return factionRepo.findById(factionId)
                .thenCompose(optionalFaction -> {
                    if (optionalFaction.isEmpty()) {
                        return CompletableFuture.completedFuture(null);
                    }
                    Faction baseFaction = optionalFaction.get();
                    return memberRepo.findByFactionId(baseFaction.factionId())
                            .thenApply(baseFaction::withMembers);
                });
    }
}
