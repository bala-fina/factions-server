package dev.balafini.factions.model.faction;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record Faction(
        @JsonProperty("_id") UUID factionId,
        String name,
        String tag,
        Instant createdAt,
        double power,
        double maxPower,
        double kdr,

        @JsonIgnore Set<FactionMember> members
) {

    public static final String FACTION_NAME_PATTERN = "^[a-zA-Z0-9]{6,16}$";
    public static final String FACTION_TAG_PATTERN = "^[a-zA-Z0-9]{3,4}$";

    public static Faction create(String name, String tag, UUID leaderId, double leaderPower, double leaderMaxPower, double leaderKdr) {
        UUID factionId = UUID.randomUUID();
        FactionMember leader = FactionMember.create(leaderId, FactionMember.FactionRole.LEADER, factionId);
        return new Faction(
                factionId,
                name,
                tag,
                Instant.now(),
                leaderPower,
                leaderMaxPower,
                leaderKdr,
                Set.of(leader)
        );
    }

    public Faction withMembers(Set<FactionMember> members) {
        return new Faction(
                this.factionId,
                this.name,
                this.tag,
                this.createdAt,
                this.power,
                this.maxPower,
                this.kdr,
                members
        );
    }

    @JsonIgnore
    public FactionMember getLeader() {
        return members.stream()
                .filter(user -> user.role() == FactionMember.FactionRole.LEADER)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("A facção deve deve ter um líder!"));
    }

    @JsonIgnore
    public FactionMember getMember(UUID playerId) {
        return members.stream()
                .filter(member -> member.playerId().equals(playerId))
                .findFirst()
                .orElse(null);
    }
}
