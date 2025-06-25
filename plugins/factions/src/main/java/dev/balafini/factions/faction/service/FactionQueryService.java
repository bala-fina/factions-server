package dev.balafini.factions.faction.service;

import dev.balafini.factions.faction.cache.FactionCache;
import dev.balafini.factions.faction.Faction;
import dev.balafini.factions.faction.repository.FactionMemberRepository;
import dev.balafini.factions.faction.repository.FactionRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class FactionQueryService {

    private final FactionCache factionCache;
    private final FactionRepository factionRepository;
    private final FactionMemberRepository factionMemberRepository;

    public FactionQueryService(FactionCache factionCache, FactionRepository factionRepository, FactionMemberRepository factionMemberRepository) {
        this.factionCache = factionCache;
        this.factionRepository = factionRepository;
        this.factionMemberRepository = factionMemberRepository;
    }

    public CompletionStage<Optional<Faction>> findFactionById(UUID factionId) {
        return factionCache.getById(factionId);
    }

    public CompletionStage<Optional<Faction>> findFactionByName(String name) {
        return factionCache.getByName(name);
    }

    public CompletionStage<Optional<Faction>> findFactionByTag(String tag) {
        return factionCache.getByTag(tag);
    }

    public CompletionStage<Optional<Faction>> findFactionByPlayer(UUID playerId) {
        return factionMemberRepository.findByPlayerId(playerId)
                .thenCompose(optMember -> optMember
                        .map(member -> findFactionById(member.factionId()))
                        .orElse(CompletableFuture.completedFuture(Optional.empty()))
                );
    }

    public CompletionStage<Long> getFactionRank(Faction faction) {
        return factionRepository.countFactionsWithHigherKdr(faction.kdr())
                .thenApply(count -> count + 1);
    }

    public CompletionStage<List<Faction>> getTopFactionsByKdr(int limit) {
        return factionRepository.getTopKdrFactions(limit);
    }
}
