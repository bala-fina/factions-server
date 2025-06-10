package dev.balafini.factions.service;

import dev.balafini.factions.cache.FactionCache;
import dev.balafini.factions.model.Faction;
import dev.balafini.factions.model.FactionUser;
import dev.balafini.factions.repository.FactionRepository;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class FactionService {

    private final FactionCache cache;
    private final FactionRepository repository;

    public FactionService(FactionCache cache, FactionRepository repository) {
        this.cache = cache;
        this.repository = repository;
    }

    public CompletableFuture<Faction> createFaction(String name, String tag, UUID leaderId) {
        try {
            validateFactionParameters(name, tag);
        } catch (IllegalArgumentException e) {
            return CompletableFuture.failedFuture(e);
        }

        return validateFactionCreation(name, tag, leaderId)
                .thenCompose(valid -> {
                    UUID factionId = UUID.randomUUID();
                    FactionUser leader = new FactionUser(leaderId, factionId,
                            FactionUser.FactionRole.LEADER, Instant.now());

                    Faction faction = new Faction(factionId, name, tag,
                            Instant.now(), Set.of(leader));

                    cache.put(faction);
                    return repository.save(faction).thenApply(v -> faction);
                });
    }

    public CompletableFuture<Optional<Faction>> getFactionById(UUID id) {
        Optional<Faction> cached = cache.getById(id);
        if (cached.isPresent()) {
            return CompletableFuture.completedFuture(cached);
        }

        return repository.findById(id)
                .thenApply(faction -> {
                    faction.ifPresent(cache::put);
                    return faction;
                });
    }

    public CompletableFuture<Optional<Faction>> getFactionByName(String name) {
        Optional<Faction> cached = cache.getByName(name);
        if (cached.isPresent()) {
            return CompletableFuture.completedFuture(cached);
        }

        return repository.findByName(name)
                .thenApply(faction -> {
                    faction.ifPresent(cache::put);
                    return faction;
                });
    }

    public CompletableFuture<Optional<Faction>> getFactionByTag(String tag) {
        Optional<Faction> cached = cache.getByTag(tag);
        if (cached.isPresent()) {
            return CompletableFuture.completedFuture(cached);
        }

        return repository.findByTag(tag)
                .thenApply(faction -> {
                    faction.ifPresent(cache::put);
                    return faction;
                });
    }

    public CompletableFuture<Optional<Faction>> getPlayerFaction(UUID playerId) {
        Optional<Faction> cached = cache.getByPlayer(playerId);
        if (cached.isPresent()) {
            return CompletableFuture.completedFuture(cached);
        }

        return repository.findByPlayer(playerId)
                .thenApply(faction -> {
                    faction.ifPresent(cache::put);
                    return faction;
                });
    }

    public CompletableFuture<Void> disbandFaction(UUID factionId, UUID leaderId) {
        return getFactionById(factionId)
                .thenCompose(optFaction -> {
                    Faction faction = requireFaction(optFaction.orElse(null));

                    if (!faction.getLeader().playerId().equals(leaderId)) {
                        throw new IllegalArgumentException("Only leader can disband faction");
                    }

                    cache.remove(factionId);
                    return repository.delete(factionId);
                });
    }

    public CompletableFuture<Faction> addMember(UUID factionId, UUID playerId) {
        return getFactionById(factionId)
                .thenCompose(optFaction -> {
                    Faction faction = requireFaction(optFaction.orElse(null));

                    return getPlayerFaction(playerId)
                            .thenApply(optPlayerFaction -> {
                                if (optPlayerFaction.isPresent()) {
                                    throw new IllegalArgumentException("Player already in faction");
                                }

                                FactionUser newMember = new FactionUser(playerId, factionId,
                                        FactionUser.FactionRole.RECRUIT, Instant.now());

                                Set<FactionUser> members = new HashSet<>(faction.members());
                                members.add(newMember);

                                Faction updated = new Faction(faction.id(), faction.name(),
                                        faction.tag(), faction.createdAt(), members);

                                cache.put(updated);
                                return updated;
                            })
                            .thenCompose(updated -> repository.save(updated).thenApply(v -> updated));
                });
    }

    public CompletableFuture<Faction> removeMember(UUID factionId, UUID requesterId, UUID targetId) {
        return getFactionById(factionId)
                .thenCompose(optFaction -> {
                    Faction faction = requireFaction(optFaction.orElse(null));

                    FactionUser requester = faction.getMember(requesterId);
                    FactionUser target = faction.getMember(targetId);

                    if (requester == null || target == null) {
                        throw new IllegalArgumentException("Player not in faction");
                    }

                    validateRemoval(requester, target);

                    Set<FactionUser> members = new HashSet<>(faction.members());
                    members.remove(target);

                    Faction updated = new Faction(faction.id(), faction.name(),
                            faction.tag(), faction.createdAt(), members);

                    cache.put(updated);
                    return repository.save(updated).thenApply(v -> updated);
                });
    }

    public CompletableFuture<Faction> promotePlayer(UUID factionId, UUID promoterId, UUID targetId) {
        return changeRole(factionId, promoterId, targetId, true);
    }

    public CompletableFuture<Faction> demotePlayer(UUID factionId, UUID demoterId, UUID targetId) {
        return changeRole(factionId, demoterId, targetId, false);
    }

    private CompletableFuture<Faction> changeRole(UUID factionId, UUID requesterId, UUID targetId, boolean promote) {
        return getFactionById(factionId)
                .thenCompose(optFaction -> {
                    Faction faction = requireFaction(optFaction.orElse(null));

                    FactionUser requester = faction.getMember(requesterId);
                    FactionUser target = faction.getMember(targetId);

                    if (requester == null || target == null) {
                        throw new IllegalArgumentException("Player not in faction");
                    }

                    FactionUser.FactionRole newRole = promote ?
                            getNextHigherRole(target.role()) : getNextLowerRole(target.role());

                    if (newRole == null) {
                        throw new IllegalArgumentException("Cannot " +
                                                           (promote ? "promote" : "demote") + " further");
                    }

                    if (!canChangeRole(requester.role(), target.role(), newRole)) {
                        throw new IllegalArgumentException("Insufficient permissions");
                    }

                    Set<FactionUser> members = new HashSet<>(faction.members());
                    members.remove(target);
                    members.add(new FactionUser(targetId, factionId, newRole, target.joinedAt()));

                    Faction updated = new Faction(faction.id(), faction.name(),
                            faction.tag(), faction.createdAt(), members);

                    cache.put(updated);
                    return repository.save(updated).thenApply(v -> updated);
                });
    }

    private CompletableFuture<Boolean> validateFactionCreation(String name, String tag, UUID leaderId) {
        CompletableFuture<Boolean> nameCheck = repository.existsByName(name);
        CompletableFuture<Boolean> tagCheck = repository.existsByTag(tag);
        CompletableFuture<Optional<Faction>> playerCheck = getPlayerFaction(leaderId);

        return nameCheck.thenCombine(tagCheck, (nameExists, tagExists) -> {
            if (nameExists) {
                throw new IllegalArgumentException("Faction name already exists");
            }
            if (tagExists) {
                throw new IllegalArgumentException("Faction tag already exists");
            }
            return true;
        }).thenCombine(playerCheck, (valid, playerFaction) -> {
            if (playerFaction.isPresent()) {
                throw new IllegalArgumentException("Player already in faction");
            }
            return true;
        });
    }

    private void validateFactionParameters(String name, String tag) {
        if (!name.matches(Faction.FACTION_NAME_PATTERN)) {
            throw new IllegalArgumentException("Name must be 6-12 alphanumeric characters");
        }
        if (!tag.matches(Faction.FACTION_TAG_PATTERN)) {
            throw new IllegalArgumentException("Tag must be 3-4 alphanumeric characters");
        }
    }


    private void validateRemoval(FactionUser requester, FactionUser target) {
        if (target.role() == FactionUser.FactionRole.LEADER) {
            throw new IllegalArgumentException("Cannot remove leader");
        }
        if (!requester.role().canManage(target.role())) {
            throw new IllegalArgumentException("Insufficient permissions");
        }
    }

    private boolean canChangeRole(FactionUser.FactionRole requester,
                                  FactionUser.FactionRole current,
                                  FactionUser.FactionRole target) {
        return requester.canManage(current) && requester.canManage(target);
    }

    private FactionUser.FactionRole getNextHigherRole(FactionUser.FactionRole role) {
        return switch (role) {
            case RECRUIT -> FactionUser.FactionRole.MEMBER;
            case MEMBER -> FactionUser.FactionRole.OFFICER;
            default -> null;
        };
    }

    private FactionUser.FactionRole getNextLowerRole(FactionUser.FactionRole role) {
        return switch (role) {
            case OFFICER -> FactionUser.FactionRole.MEMBER;
            case MEMBER -> FactionUser.FactionRole.RECRUIT;
            default -> null;
        };
    }

    private Faction requireFaction(Faction faction) {
        if (faction == null) {
            throw new IllegalArgumentException("Faction not found");
        }
        return faction;
    }
}