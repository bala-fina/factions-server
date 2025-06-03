package dev.balafini.factions.service;

import dev.balafini.factions.cache.FactionCache;
import dev.balafini.factions.model.Faction;
import dev.balafini.factions.model.FactionUser;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class FactionService {

    private final FactionCache cache;

    public FactionService(FactionCache cache) {
        this.cache = cache;
    }

    public Faction createFaction(String name, String tag, UUID leaderId) {
        if (cache.existsByName(name)) {
            throw new IllegalArgumentException("Faction name already exists");
        }
        if (cache.existsByTag(tag)) {
            throw new IllegalArgumentException("Faction tag already exists");
        }
        if (cache.getByPlayer(leaderId).isPresent()) {
            throw new IllegalArgumentException("Player is already in a faction");
        }

        UUID factionId = UUID.randomUUID();
        FactionUser leader = new FactionUser(
                leaderId,
                factionId,
                FactionUser.FactionRole.LEADER,
                Instant.now()
        );

        Faction faction = new Faction(
                factionId,
                name,
                tag,
                Instant.now(),
                Set.of(leader)
        );

        cache.put(faction);
        return faction;
    }

    public Optional<Faction> getFactionById(UUID id) {
        return cache.getById(id);
    }

    public Optional<Faction> getFactionByName(String name) {
        return cache.getByName(name);
    }

    public Optional<Faction> getFactionByTag(String tag) {
        return cache.getByTag(tag);
    }

    public Optional<Faction> getPlayerFaction(UUID playerId) {
        return cache.getByPlayer(playerId);
    }

    public Faction addMember(UUID factionId, UUID playerId) {
        Faction faction = cache.getById(factionId)
                .orElseThrow(() -> new IllegalArgumentException("Faction not found"));

        if (cache.getByPlayer(playerId).isPresent()) {
            throw new IllegalArgumentException("Player is already in a faction");
        }

        FactionUser newMember = new FactionUser(
                playerId,
                factionId,
                FactionUser.FactionRole.MEMBER,
                Instant.now()
        );

        Set<FactionUser> updatedMembers = new HashSet<>(faction.members());
        updatedMembers.add(newMember);

        Faction updatedFaction = new Faction(
                faction.id(),
                faction.name(),
                faction.tag(),
                faction.createdAt(),
                updatedMembers
        );

        cache.put(updatedFaction);
        return updatedFaction;
    }

    public Faction removeMember(UUID factionId, UUID requesterId, UUID targetPlayerId) {
        Faction faction = cache.getById(factionId)
                .orElseThrow(() -> new IllegalArgumentException("Faction not found"));

        if (!faction.isMember(requesterId)) {
            throw new IllegalArgumentException("Requester is not a member of this faction");
        }

        if (!faction.isMember(targetPlayerId)) {
            throw new IllegalArgumentException("Target player is not a member of this faction");
        }

        FactionUser requester = faction.members().stream()
                .filter(member -> member.playerId().equals(requesterId))
                .findFirst()
                .orElseThrow();

        FactionUser target = faction.members().stream()
                .filter(member -> member.playerId().equals(targetPlayerId))
                .findFirst()
                .orElseThrow();

        if (!canRemoveMember(requester.role(), target.role())) {
            throw new IllegalArgumentException("Insufficient permissions to remove this member");
        }

        if (target.role() == FactionUser.FactionRole.LEADER) {
            throw new IllegalArgumentException("Cannot remove faction leader");
        }

        Set<FactionUser> updatedMembers = faction.members().stream()
                .filter(member -> !member.playerId().equals(targetPlayerId))
                .collect(java.util.stream.Collectors.toSet());

        Faction updatedFaction = new Faction(
                faction.id(),
                faction.name(),
                faction.tag(),
                faction.createdAt(),
                updatedMembers
        );

        cache.put(updatedFaction);
        return updatedFaction;
    }

    private boolean canRemoveMember(FactionUser.FactionRole requesterRole, FactionUser.FactionRole targetRole) {
        if (requesterRole.getLevel() > FactionUser.FactionRole.OFFICER.getLevel()) {
            return false;
        }

        if (requesterRole == FactionUser.FactionRole.OFFICER && targetRole == FactionUser.FactionRole.OFFICER) {
            return false;
        }

        return requesterRole.getLevel() < targetRole.getLevel();
    }

    public void disbandFaction(UUID factionId, UUID leaderId) {
        Faction faction = cache.getById(factionId)
                .orElseThrow(() -> new IllegalArgumentException("Faction not found"));

        FactionUser leader = faction.getLeader();
        if (!leader.playerId().equals(leaderId)) {
            throw new IllegalArgumentException("Only the faction leader can disband the faction");
        }

        cache.remove(factionId);
    }
}
