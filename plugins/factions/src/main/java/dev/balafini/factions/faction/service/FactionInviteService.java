package dev.balafini.factions.faction.service;

import dev.balafini.factions.faction.exception.*;
import dev.balafini.factions.faction.Faction;
import dev.balafini.factions.faction.FactionInvite;
import dev.balafini.factions.faction.FactionMember;
import dev.balafini.factions.faction.repository.FactionInviteRepository;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/*
    TODO: Refactor to use FactionQueryService and FactionLifecycleService
 */
public class FactionInviteService {

    private final FactionInviteRepository inviteRepository;
    private final FactionQueryService factionQueryService;
    private final FactionMembershipService factionMembershipService;


    public FactionInviteService(FactionInviteRepository inviteRepository, FactionQueryService factionQueryService, FactionMembershipService factionMembershipService) {
        this.inviteRepository = inviteRepository;
        this.factionQueryService = factionQueryService;
        this.factionMembershipService = factionMembershipService;
    }

    public CompletionStage<FactionInvite> invitePlayer(UUID inviterId, UUID inviteeId) {
        return factionQueryService.findFactionByPlayer(inviterId)
                .thenCompose(optInviterFaction -> {
                    if (optInviterFaction.isEmpty()) {
                        throw new PlayerNotInFactionException("Você precisa estar em uma facção para convidar jogadores.");
                    }
                    Faction inviterFaction = optInviterFaction.get();
                    FactionMember inviter = inviterFaction.getMember(inviterId);

                    if (inviter == null || !inviter.role().canInvite()) {
                        throw new InsufficientPermissionException("Você não tem permissão para convidar jogadores.");
                    }

                    return factionQueryService.findFactionByPlayer(inviteeId)
                            .thenCompose(optInviteeFaction -> {
                                if (optInviteeFaction.isPresent()) {
                                    throw new PlayerAlreadyInFactionException("Este jogador já está em uma facção.");
                                }

                                FactionInvite invite = FactionInvite.create(inviterFaction.tag(), inviterId, inviteeId);
                                return inviteRepository.insert(invite).thenApply(_ -> invite);
                            });
                });
    }

    public CompletionStage<Faction> acceptInvite(UUID inviteeId, String inviteeName, String factionTag) {
        return inviteRepository.findByInviteeAndTag(inviteeId, factionTag)
                .thenCompose(optInvite -> {
                    if (optInvite.isEmpty()) {
                        throw new PlayerNotInvitedException("Você não tem um convite para essa facção!");
                    }

                    return factionQueryService.findFactionByTag(factionTag).thenCompose(optFaction -> {
                        if (optFaction.isEmpty()) {
                            throw new FactionNotFoundException("A facção que você está tentando entrar não existe.");
                        }
                        Faction faction = optFaction.get();

                        return factionMembershipService.addMember(faction.factionId(), inviteeId, inviteeName)
                                .thenCompose(_ -> inviteRepository.deleteByInviteeId(inviteeId))
                                .thenApply(_ -> faction);
                    });
                });
    }

    public CompletionStage<Void> denyInvite(UUID inviteeId, String factionTag) {
        return inviteRepository.findByInviteeAndTag(inviteeId, factionTag)
                .thenCompose(optInvite -> optInvite
                        .map(invite -> inviteRepository.deleteById(invite.id()).thenAccept(_ -> {
                        }))
                        .orElseGet(() -> CompletableFuture.failedFuture(
                                new PlayerNotInvitedException("Você não tem um convite para essa facção!")))
                );
    }
}

