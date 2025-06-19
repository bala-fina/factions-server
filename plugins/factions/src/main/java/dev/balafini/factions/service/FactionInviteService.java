package dev.balafini.factions.service;

import dev.balafini.factions.model.faction.Faction;
import dev.balafini.factions.model.faction.FactionInvite;
import dev.balafini.factions.repository.faction.FactionInviteRepository;
import dev.balafini.factions.repository.faction.FactionRepository;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class FactionInviteService {

    private final FactionInviteRepository inviteRepository;
    private final FactionRepository factionRepository;
    private final FactionService factionService;


    public FactionInviteService(FactionInviteRepository inviteRepository, FactionRepository factionRepository, FactionService factionService) {
        this.inviteRepository = inviteRepository;
        this.factionRepository = factionRepository;
        this.factionService = factionService;
    }

    public CompletionStage<FactionInvite> createInvite(String factionTag, UUID inviterId, UUID inviteeId) {
        return factionService.findFactionByPlayer(inviteeId)
                .thenCompose(optPlayerFaction -> {
                    if (optPlayerFaction.isPresent()) {
                        return CompletableFuture.failedFuture(new IllegalStateException("Esse jogador já está em uma facção!"));
                    }

                    FactionInvite invite = FactionInvite.create(factionTag, inviterId, inviteeId);
                    return inviteRepository.save(invite).thenApply(v -> invite);
                });
    }

    public CompletionStage<Faction> acceptInvite(UUID inviteeId, String factionTag) {
        return inviteRepository.findByInviteeAndTag(inviteeId, factionTag)
                .thenCompose(optInvite -> {
                    if (optInvite.isEmpty()) {
                        return CompletableFuture.failedFuture(new IllegalArgumentException("Você não tem um convite para essa facção!"));
                    }

                    return factionRepository.findByTag(factionTag).thenCompose(optFaction -> {
                        if (optFaction.isEmpty()) {
                            return CompletableFuture.failedFuture(new IllegalStateException("A facção que você está tentando entrar não existe!"));
                        }
                        Faction faction = optFaction.get();

                        return factionService.addMember(faction.factionId(), inviteeId)
                                .thenCompose(v -> inviteRepository.deleteByInviteeId(inviteeId).thenApply(deletedCount -> faction));
                    });
                });
    }

    public CompletionStage<Void> denyInvite(UUID inviteeId, String factionTag) {
        return inviteRepository.findByInviteeAndTag(inviteeId, factionTag)
                .thenCompose(optInvite -> optInvite
                        .map(invite -> inviteRepository.deleteById(invite.id()).thenAccept(__ -> {}))
                        .orElseGet(() -> CompletableFuture.failedFuture(
                                new IllegalArgumentException("Você não tem um convite para essa facção!")))
                );
    }
}

