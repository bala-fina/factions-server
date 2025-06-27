package dev.balafini.factions.faction.cache;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.balafini.factions.faction.FactionClaim;
import dev.balafini.factions.faction.FactionMember;
import dev.balafini.factions.faction.repository.FactionClaimRepository;
import dev.balafini.factions.faction.repository.FactionMemberRepository;
import org.bukkit.Chunk;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

public class FactionMemberCache {
    private final AsyncLoadingCache<UUID, FactionMember> cacheById;
    private final FactionMemberRepository factionMemberRepository;

    public FactionMemberCache(FactionMemberRepository factionMemberRepository, Executor executor) {
        this.factionMemberRepository = factionMemberRepository;
        this.cacheById = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .executor(executor)
            .buildAsync((id, _) ->
                factionMemberRepository.findByPlayerId(id)
                    .thenApply(opt -> opt.orElse(null))
                    .toCompletableFuture());
    }

    public CompletableFuture<Optional<FactionMember>> getByPlayerId(UUID id) {
        if (id == null) return CompletableFuture.completedFuture(Optional.empty());

        final var cachedMember = Optional.ofNullable(cacheById.getIfPresent(id));
        return cachedMember
            .map(memberCompletableFuture -> memberCompletableFuture.thenApply(Optional::ofNullable))
            .orElseGet(() -> factionMemberRepository.findByPlayerId(id).thenApply(optMember -> {
                optMember.ifPresent(this::put);
                return optMember;
            }));
    }

    public void put(FactionMember member) {
        cacheById.put(member.playerId(), CompletableFuture.completedFuture(member));
    }

    public void invalidate(UUID memberId) {
        if (memberId == null) return;
        invalidateById(memberId);
    }

    public void invalidateById(UUID id) {
        cacheById.synchronous().invalidate(id);
    }
}
