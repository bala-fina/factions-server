package dev.balafini.factions.faction;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Sets;
import dev.balafini.factions.FactionsPlugin;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public record Faction(
    @JsonProperty UUID factionId,
    String name,
    String tag,
    Instant createdAt,
    double power,
    double maxPower,
    double kdr,
    Set<UUID> memberIds,
    Set<UUID> claimIds
) {

    public static Faction create(String name, String tag, UUID leaderId, double leaderPower, double leaderMaxPower, double leaderKdr) {
        UUID factionId = UUID.randomUUID();
        return new Faction(
            factionId,
            name,
            tag,
            Instant.now(),
            leaderPower,
            leaderMaxPower,
            leaderKdr,
            Set.of(leaderId),
            Sets.newHashSet()
        );
    }

    @JsonIgnore
    public Set<FactionMember> getMembers() {
        return memberIds.stream()
            .map(memberId -> FactionsPlugin.getInstance().getFactionMemberCache().getByPlayerId(memberId))
            .map(CompletableFuture::join)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toSet());
    }

    @JsonIgnore
    public FactionMember getLeader() {
        return getMembers().stream()
            .filter(user -> user.role() == FactionMember.FactionRole.LEADER)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("A facção deve deve ter um líder!"));
    }

    @JsonIgnore
    public FactionMember getMember(UUID playerId) {
        return getMembers().stream()
            .filter(member -> member.playerId().equals(playerId))
            .findFirst()
            .orElse(null);
    }

    public boolean hasMember(UUID playerId) {
        return memberIds.contains(playerId);
    }

    public void removeMember(UUID playerId) {
        if (playerId == null || !memberIds.contains(playerId)) return;

        memberIds.remove(playerId);
    }

    public void addMember(UUID playerId) {
        if (playerId == null || memberIds.contains(playerId)) return;

        memberIds.add(playerId);
    }

    public void addClaim(FactionClaim claim) {
        if (claim == null || claimIds.contains(claim.getId())) return;

        claimIds.add(claim.getId());
    }

}
