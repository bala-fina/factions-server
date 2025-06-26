package dev.balafini.factions.faction.service;

import dev.balafini.factions.faction.FactionClaim;
import dev.balafini.factions.faction.FactionMember;
import dev.balafini.factions.faction.cache.FactionClaimCache;
import dev.balafini.factions.faction.exception.*;
import dev.balafini.factions.faction.Faction;
import dev.balafini.factions.faction.repository.FactionClaimRepository;
import dev.balafini.factions.faction.validator.FactionClaimValidator;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

public class FactionClaimService {

    private final FactionClaimValidator claimValidator;
    private final FactionClaimCache factionClaimCache;
    private final FactionClaimRepository factionClaimRepository;

    private final FactionQueryService factionQueryService;

    public FactionClaimService(
        FactionClaimValidator claimValidator,
        FactionClaimCache claimCache,
        FactionClaimRepository claimRepository,
        FactionQueryService factionQueryService
    ) {
        this.factionClaimRepository = claimRepository;
        this.factionClaimCache = claimCache;
        this.claimValidator = claimValidator;
        this.factionQueryService = factionQueryService;
    }

    public CompletableFuture<FactionClaim> createClaim(Faction claimingFaction, Player claimingPlayer, Chunk chunk, FactionClaim.ClaimType type) {
        final var claim = FactionClaim.createClaim(chunk, type, claimingFaction.factionId(), claimingPlayer.getUniqueId());

        return factionClaimRepository.insert(claim);
    }

    public CompletableFuture<Boolean> removeClaim(Chunk chunk) {
        return factionClaimRepository.findByChunk(chunk)
            .thenCompose(optClaim -> {
                if (optClaim.isEmpty()) {
                    throw new FactionClaimNotFoundException("Este terreno não está reivindicado pela facção.");
                }

                FactionClaim claim = optClaim.get();
                return factionClaimRepository.deleteById(claim.id());
            });
    }

    public CompletableFuture<FactionClaim> claimChunk(Player player, Chunk chunk) {
        try {
            return factionQueryService.findFactionByPlayer(player.getUniqueId())
                .thenCompose(faction -> {
                    if (faction.isEmpty()) {
                        throw new PlayerNotInFactionException("Você não está em uma facção.");
                    }

                    final var factionMember = faction.get().getMember(player.getUniqueId());
                    if (factionMember.role().isLowerThanOrEqualTo(FactionMember.FactionRole.MEMBER)) {
                        throw new InsufficientPermissionException("Apenas um capitão ou superior da facção pode reivindicar terrenos.");
                    }

                    return CompletableFuture.completedFuture(faction.get());
                })
                .thenCompose(faction -> {
                    final var claim = FactionClaim.createClaim(chunk, FactionClaim.ClaimType.PLAYER, faction.factionId(), player.getUniqueId());
                    return factionClaimRepository.insert(claim);
                })
                .thenApply(claim -> {
                    factionClaimCache.put(claim);
                    return claim;
                });
        } catch (InvalidFactionClaimException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

}

