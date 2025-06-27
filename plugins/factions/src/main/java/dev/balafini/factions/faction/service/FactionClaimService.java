package dev.balafini.factions.faction.service;

import dev.balafini.factions.faction.FactionClaim;
import dev.balafini.factions.faction.FactionMember;
import dev.balafini.factions.faction.cache.FactionClaimCache;
import dev.balafini.factions.faction.exception.*;
import dev.balafini.factions.faction.Faction;
import dev.balafini.factions.faction.repository.FactionClaimRepository;
import dev.balafini.factions.faction.repository.FactionRepository;
import dev.balafini.factions.faction.validator.FactionClaimValidator;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class FactionClaimService {

    private final FactionClaimValidator claimValidator;
    private final FactionClaimCache factionClaimCache;
    private final FactionClaimRepository factionClaimRepository;

    private final FactionRepository factionRepository;

    private final FactionQueryService factionQueryService;

    public FactionClaimService(
        FactionClaimValidator claimValidator,
        FactionClaimCache claimCache,
        FactionClaimRepository claimRepository,
        FactionRepository factionRepository,
        FactionQueryService factionQueryService
    ) {
        this.factionClaimRepository = claimRepository;
        this.factionClaimCache = claimCache;
        this.claimValidator = claimValidator;
        this.factionRepository = factionRepository;
        this.factionQueryService = factionQueryService;
    }

    public CompletableFuture<FactionClaim> claimChunk(Player player, Chunk chunk) {
        return factionQueryService.findFactionByPlayer(player.getUniqueId())
            .thenCompose(optionalFaction -> validatePlayerFaction(player, optionalFaction))
            .thenCompose(faction -> validateInexistentClaim(faction, chunk))
            .thenCompose(faction -> {
                if (faction != null) {
                    return createAndStoreClaim(faction, chunk, player);
                }

                throw new InvalidFactionClaimException("Não foi possível reivindicar o terreno.");
            });
    }

    // Internal methods for validation and claim creation

    private CompletableFuture<Faction> validatePlayerFaction(Player player, Optional<Faction> optionalFaction) {
        if (optionalFaction.isEmpty()) {
            return CompletableFuture.failedFuture(
                new PlayerNotInFactionException("Você não está em uma facção.")
            );
        }

        Faction faction = optionalFaction.get();
        FactionMember member = faction.getMember(player.getUniqueId());

        if (member.role().isLowerThanOrEqualTo(FactionMember.FactionRole.MEMBER)) {
            return CompletableFuture.failedFuture(
                new InsufficientPermissionException("Apenas um capitão ou superior da facção pode reivindicar terrenos.")
            );
        }

        return CompletableFuture.completedFuture(faction);
    }

    private CompletableFuture<Faction> validateInexistentClaim(Faction faction, Chunk chunk) {
        return claimValidator.validateChunkClaim(faction, chunk)
            .thenCompose(_ ->
                factionClaimRepository.findByChunk(chunk)
                    .thenApply(optExistingClaim -> {
                        if (optExistingClaim.isPresent()) {
                            throw new InvalidFactionClaimException("Este terreno já está reivindicado por outra facção.");
                        }

                        return faction;
                    })
            );
    }

    private CompletableFuture<FactionClaim> createAndStoreClaim(Faction faction, Chunk chunk, Player player) {
        FactionClaim claim = FactionClaim.createClaim(
            chunk,
            FactionClaim.ClaimType.PLAYER,
            faction.factionId(),
            player.getUniqueId()
        );

        return factionClaimRepository.insert(claim)
            .thenApply(factionClaimCache::put)
            .whenComplete((_, throwable) -> {
                if (throwable != null) {
                    faction.addClaim(claim);
                    factionRepository.upsert(faction);
                }
            });
    }

}

