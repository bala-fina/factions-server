package dev.balafini.factions.cache;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.balafini.factions.model.user.User;
import dev.balafini.factions.repository.user.UserRepository;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

public class UserCache {
    private final AsyncLoadingCache<UUID, User> cacheById;
    private final Cache<String, UUID> displayNameToIdCache;
    private final UserRepository userRepository;

    public UserCache(UserRepository userRepository, Executor executor) {
        this.userRepository = userRepository;
        this.cacheById = Caffeine.newBuilder()
                .maximumSize(5000)
                .expireAfterAccess(Duration.ofHours(2))
                .executor(executor)
                .buildAsync((id, exec) ->
                        userRepository.findByPlayerId(id)
                                .thenApply(optUser -> optUser.orElse(null))
                                .toCompletableFuture()
                );

        this.displayNameToIdCache = Caffeine.newBuilder()
                .maximumSize(10000)
                .expireAfterAccess(Duration.ofHours(2))
                .build();
    }

    public CompletionStage<Optional<User>> getById(UUID id) {
        if (id == null) return CompletableFuture.completedFuture(Optional.empty());
        return cacheById.get(id).thenApply(Optional::ofNullable);
    }

    public CompletionStage<Optional<User>> getByName(String displayName) {
        UUID userId = displayNameToIdCache.getIfPresent(displayName);
        if (userId != null) {
            return getById(userId);
        }
        return userRepository.findByPlayerName(displayName)
                .thenApply(optUser -> {
                    optUser.ifPresent(this::put);
                    return optUser;
                });
    }

    public void put(User user) {
        cacheById.put(user.playerId(), CompletableFuture.completedFuture(user));
        displayNameToIdCache.put(user.displayName(), user.playerId());
    }

    public void invalidate(User user) {
        if (user == null) return;
        cacheById.synchronous().invalidate(user.playerId());
        displayNameToIdCache.invalidate(user.displayName());
    }

    public void invalidateById(UUID id) {
        cacheById.synchronous().invalidate(id);
    }
}
