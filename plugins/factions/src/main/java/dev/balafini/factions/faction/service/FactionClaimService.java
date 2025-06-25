package dev.balafini.factions.faction.service;

import dev.balafini.factions.faction.FactionClaim;
import dev.balafini.factions.faction.exception.*;
import dev.balafini.factions.faction.Faction;
import dev.balafini.factions.faction.repository.FactionClaimRepository;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletionStage;

public class FactionClaimService {

    private final FactionClaimRepository claimRepository;

    public FactionClaimService(FactionClaimRepository claimRepository) {
        this.claimRepository = claimRepository;
    }

    public CompletionStage<Void> createClaim(Faction claimingFaction, Player claimingPlayer, Chunk chunk, FactionClaim.ClaimType type) {
        final var claim = FactionClaim.createClaim(chunk, type, claimingFaction.factionId(), claimingPlayer.getUniqueId());

        return claimRepository.insert(claim);
    }

    public CompletionStage<Boolean> removeClaim(Chunk chunk) {
        return claimRepository.findByChunk(chunk)
                .thenCompose(optClaim -> {
                    if (optClaim.isEmpty()) {
                        throw new FactionClaimNotFoundException("Este terreno não está reivindicado pela facção.");
                    }

                    FactionClaim claim = optClaim.get();
                    return claimRepository.deleteById(claim.id());
                });
    }



}

