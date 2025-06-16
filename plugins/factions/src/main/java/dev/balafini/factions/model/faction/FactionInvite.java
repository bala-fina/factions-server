package dev.balafini.factions.model.faction;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import java.time.Instant;
import java.util.UUID;

public record FactionInvite(
        @Id
        @ObjectId
        String id,

        String factionTag,
        UUID inviterId,
        UUID inviteeId,
        Instant invitedAt
) {

    public static FactionInvite create(String factionTag, UUID inviterId, UUID inviteeId) {
        if (inviterId.equals(inviteeId)) {
            throw new IllegalArgumentException("Você não pode se convidar para uma facção!");
        }

        return new FactionInvite(
                null,
                factionTag,
                inviterId,
                inviteeId,
                Instant.now()
        );
    }
}
