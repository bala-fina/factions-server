package dev.balafini.factions.faction.validator;

import dev.balafini.factions.faction.Faction;
import dev.balafini.factions.faction.exception.FactionAlreadyExistsException;
import dev.balafini.factions.faction.exception.InvalidFactionParametersException;
import dev.balafini.factions.faction.exception.PlayerAlreadyInFactionException;
import dev.balafini.factions.faction.repository.FactionClaimRepository;
import dev.balafini.factions.faction.repository.FactionMemberRepository;
import dev.balafini.factions.faction.repository.FactionRepository;
import org.bukkit.Chunk;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class FactionChunkValidator {

    private final FactionClaimRepository claimRepository;

    public FactionChunkValidator(FactionClaimRepository claimRepository) {
        this.claimRepository = claimRepository;
    }

    public CompletionStage<Void> validateChunkClaim(Faction faction, Chunk chunk) {
        CompletionStage<Boolean> claimedChunkCheck = claimRepository.findByChunk(chunk).thenApply(Optional::isPresent);
        return CompletableFuture.allOf(claimedChunkCheck.toCompletableFuture())
            .thenRun(() -> {
                if (claimedChunkCheck.toCompletableFuture().join()) {
                    throw new InvalidFactionParametersException("Este terreno já está reivindicado por outra facção.");
                }
            });
    }
}
