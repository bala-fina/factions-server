package dev.balafini.factions.service.faction;

import dev.balafini.factions.cache.FactionCache;
import dev.balafini.factions.repository.faction.FactionMemberRepository;
import dev.balafini.factions.repository.faction.FactionRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class FactionStatsService {

    private final FactionRepository factionRepository;
    private final FactionMemberRepository factionMemberRepository;
    private final FactionCache factionCache;

    public FactionStatsService(FactionRepository factionRepository, FactionMemberRepository factionMemberRepository, FactionCache factionCache) {
        this.factionRepository = factionRepository;
        this.factionMemberRepository = factionMemberRepository;
        this.factionCache = factionCache;
    }

    public CompletionStage<Void> syncFactionData(UUID playerId, double powerDelta, double maxPowerDelta, double kdrDelta) {
        if (powerDelta == 0 && maxPowerDelta == 0 && kdrDelta == 0) {
            return CompletableFuture.completedFuture(null);
        }
        return factionMemberRepository.findByPlayerId(playerId).thenCompose(optMember -> {
            if (optMember.isPresent()) {
                UUID factionId = optMember.get().factionId();
                List<CompletionStage<?>> updates = new ArrayList<>();

                if (powerDelta != 0 || maxPowerDelta != 0) {
                    updates.add(factionRepository.updatePower(factionId, powerDelta, maxPowerDelta));
                }
                if (kdrDelta != 0) {
                    updates.add(factionRepository.updateKdr(factionId, kdrDelta));
                }

                CompletableFuture<?>[] futures = updates.stream()
                        .map(CompletionStage::toCompletableFuture)
                        .toArray(CompletableFuture[]::new);

                return CompletableFuture.allOf(futures)
                        .thenRun(() -> factionCache.invalidate(factionId));
            }
            return CompletableFuture.completedFuture(null);
        });
    }
}
