package dev.balafini.factions.cache;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
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
    private final AsyncLoadingCache<UUID, User> cache;

    public UserCache(UserRepository userRepository, Executor executor) {
        this.cache = Caffeine.newBuilder()
                .maximumSize(2000)
                .expireAfterAccess(Duration.ofHours(1))
                .executor(executor)
                .buildAsync((id, exec) ->
                        userRepository.findByPlayerId(id)
                                .thenApply(optUser -> optUser.orElse(null))
                                .toCompletableFuture()
                );
    }

    public CompletionStage<Optional<User>> getById(UUID id) {
        return cache.get(id).thenApply(Optional::ofNullable);
    }

    public void put(User user) {
        cache.put(user.playerId(), CompletableFuture.completedFuture(user));
    }

    public void invalidate(UUID id) {
        cache.synchronous().invalidate(id);
    }
}
