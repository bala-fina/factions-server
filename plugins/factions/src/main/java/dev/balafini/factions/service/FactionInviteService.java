package dev.balafini.factions.service;

import dev.balafini.factions.cache.FactionInviteCache;
import dev.balafini.factions.model.Faction;
import dev.balafini.factions.model.FactionInvite;
import dev.balafini.factions.model.FactionUser;
import dev.balafini.factions.repository.FactionInviteRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class FactionInviteService {

    private final FactionInviteCache inviteCache;
    private final FactionInviteRepository inviteRepository;
    private final FactionService factionService;

    public FactionInviteService(FactionInviteCache inviteCache,
                                FactionInviteRepository inviteRepository,
                                FactionService factionService) {
        this.inviteCache = inviteCache;
        this.inviteRepository = inviteRepository;
        this.factionService = factionService;
    }

    public CompletableFuture<FactionInvite> createInvite(UUID factionId, UUID inviterId, UUID inviteeId) {
        return factionService.getFactionById(factionId)
                .thenCompose(optFaction -> {
                    Faction faction = optFaction.orElseThrow(() ->
                            new IllegalArgumentException("Faction not found"));

                    FactionUser inviter = faction.getMember(inviterId);
                    if (inviter == null) {
                        throw new IllegalArgumentException("Not a faction member");
                    }

                    if (!canInvite(inviter.role())) {
                        throw new IllegalArgumentException("Insufficient permissions to invite");
                    }

                    return factionService.getPlayerFaction(inviteeId)
                            .thenCompose(optPlayerFaction -> {
                                if (optPlayerFaction.isPresent()) {
                                    throw new IllegalArgumentException("Player already in faction");
                                }

                                return hasActiveInvite(inviteeId)
                                        .thenCompose(hasInvite -> {
                                            if (hasInvite) {
                                                throw new IllegalArgumentException("Player has pending invite");
                                            }

                                            FactionInvite invite = new FactionInvite(
                                                    UUID.randomUUID(),
                                                    factionId, inviterId, inviteeId, Instant.now());

                                            inviteCache.put(invite);
                                            return inviteRepository.save(invite)
                                                    .thenApply(v -> invite);
                                        });
                            });
                });
    }

    public CompletableFuture<Faction> acceptInvite(UUID inviteId, UUID playerId) {
        return getInviteById(inviteId)
                .thenCompose(optInvite -> {
                    FactionInvite invite = optInvite.orElseThrow(() ->
                            new IllegalArgumentException("Invite not found"));

                    if (!invite.inviteeId().equals(playerId)) {
                        throw new IllegalArgumentException("Invite not for you");
                    }

                    return factionService.getPlayerFaction(playerId)
                            .thenCompose(optFaction -> {
                                if (optFaction.isPresent()) {
                                    throw new IllegalArgumentException("Already in faction");
                                }

                                inviteCache.remove(inviteId);
                                return inviteRepository.delete(inviteId)
                                        .thenCompose(v -> factionService.addMember(invite.factionId(), playerId));
                            });
                });
    }

    public CompletableFuture<Void> declineInvite(UUID inviteId, UUID playerId) {
        return getInviteById(inviteId)
                .thenCompose(optInvite -> {
                    FactionInvite invite = optInvite.orElseThrow(() ->
                            new IllegalArgumentException("Invite not found"));

                    if (!invite.inviteeId().equals(playerId)) {
                        throw new IllegalArgumentException("Invite not for you");
                    }

                    inviteCache.remove(inviteId);
                    return inviteRepository.delete(inviteId);
                });
    }

    public CompletableFuture<Void> revokeInvite(UUID inviteId, UUID revokerId) {
        return getInviteById(inviteId)
                .thenCompose(optInvite -> {
                    FactionInvite invite = optInvite.orElseThrow(() ->
                            new IllegalArgumentException("Invite not found"));

                    return factionService.getFactionById(invite.factionId())
                            .thenCompose(optFaction -> {
                                Faction faction = optFaction.orElseThrow(() ->
                                        new IllegalArgumentException("Faction not found"));

                                FactionUser revoker = faction.getMember(revokerId);
                                if (revoker == null) {
                                    throw new IllegalArgumentException("Not a faction member");
                                }

                                boolean isOwnInvite = invite.inviterId().equals(revokerId);
                                if (!isOwnInvite && !canRevokeInvites(revoker.role())) {
                                    throw new IllegalArgumentException("Insufficient permissions");
                                }

                                inviteCache.remove(inviteId);
                                return inviteRepository.delete(inviteId);
                            });
                });
    }

    public CompletableFuture<Optional<FactionInvite>> getPlayerInvite(UUID playerId) {
        Optional<FactionInvite> cached = inviteCache.getByPlayer(playerId);
        if (cached.isPresent()) {
            return CompletableFuture.completedFuture(cached);
        }

        return inviteRepository.findByPlayer(playerId)
                .thenApply(invite -> {
                    invite.ifPresent(inviteCache::put);
                    return invite;
                });
    }

    public CompletableFuture<List<FactionInvite>> getFactionInvites(UUID factionId) {
        List<FactionInvite> cached = inviteCache.getByFaction(factionId);
        if (!cached.isEmpty()) {
            return CompletableFuture.completedFuture(cached);
        }

        return inviteRepository.findByFaction(factionId)
                .thenApply(invites -> {
                    invites.forEach(inviteCache::put);
                    return invites;
                });
    }

    public CompletableFuture<Optional<FactionInvite>> getInviteById(UUID inviteId) {
        Optional<FactionInvite> cached = inviteCache.getById(inviteId);
        if (cached.isPresent()) {
            return CompletableFuture.completedFuture(cached);
        }

        return inviteRepository.findById(inviteId)
                .thenApply(invite -> {
                    invite.ifPresent(inviteCache::put);
                    return invite;
                });
    }

    private CompletableFuture<Boolean> hasActiveInvite(UUID playerId) {
        if (inviteCache.hasActiveInvite(playerId)) {
            return CompletableFuture.completedFuture(true);
        }
        return inviteRepository.hasActiveInvite(playerId);
    }

    private boolean canInvite(FactionUser.FactionRole role) {
        return role.getLevel() <= FactionUser.FactionRole.OFFICER.getLevel();
    }

    private boolean canRevokeInvites(FactionUser.FactionRole role) {
        return role.getLevel() <= FactionUser.FactionRole.OFFICER.getLevel();
    }
}
