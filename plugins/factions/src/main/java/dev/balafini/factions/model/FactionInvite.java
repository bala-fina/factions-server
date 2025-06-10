package dev.balafini.factions.model;

import java.time.Instant;
import java.util.UUID;

public record FactionInvite(
        UUID inviteId,
        UUID factionId,
        UUID inviterId,
        UUID inviteeId,
        Instant createdAt
) {

    public FactionInvite {
        if (inviterId.equals(inviteeId)) {
            throw new IllegalArgumentException("Cannot invite yourself");
        }
    }
}
