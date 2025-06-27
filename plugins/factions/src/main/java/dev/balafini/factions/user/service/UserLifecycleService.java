package dev.balafini.factions.user.service;

import dev.balafini.factions.user.cache.UserCache;
import dev.balafini.factions.config.ConfigManager;
import dev.balafini.factions.user.User;
import dev.balafini.factions.user.repository.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class UserLifecycleService {

    private final UserCache userCache;
    private final UserRepository userRepository;
    private final ConfigManager config;

    public UserLifecycleService(UserCache userCache, UserRepository userRepository, ConfigManager config) {
        this.userCache = userCache;
        this.userRepository = userRepository;
        this.config = config;
    }

    public CompletionStage<User> getOrCreateUser(UUID userId, String displayName) {
        return userRepository.findByPlayerId(userId).thenCompose(optUser -> {
            if (optUser.isPresent()) {
                User existingUser = optUser.get();
                if (!existingUser.displayName().equals(displayName)) {
                    User updatedUser = existingUser.withDisplayName(displayName);
                    return userRepository.update(updatedUser).thenApply(_ -> {
                        userCache.invalidate(existingUser);
                        userCache.put(updatedUser);
                        return updatedUser;
                    });
                }
                return CompletableFuture.completedFuture(existingUser);
            } else {
                User newUser = User.createUser(userId, displayName, config.initialPlayerPower(), config.initialMaxPlayerPower());
                return userRepository.insert(newUser).thenApply(_ -> {
                    userCache.put(newUser);
                    return newUser;
                });
            }
        });
    }

    public CompletionStage<Optional<User>> findUserById(UUID playerId) {
        return userCache.getById(playerId);
    }

    public CompletionStage<Optional<User>> findUserByName(String displayName) {
        return userCache.getByName(displayName);
    }

    CompletionStage<List<User>> getTopKdrPlayers(int limit) {
        return userRepository.getTopKdrPlayers(limit);
    }

    public CompletionStage<Void> updateUserLastSeen(UUID playerId) {
        userCache.invalidateById(playerId); // Invalidate by ID is fine here
        return userRepository.updateLastSeen(playerId);
    }

}
