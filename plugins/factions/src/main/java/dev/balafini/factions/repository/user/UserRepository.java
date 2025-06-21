package dev.balafini.factions.repository.user;

import com.mongodb.client.FindIterable;
import com.mongodb.client.model.*;
import dev.balafini.factions.database.MongoManager;
import dev.balafini.factions.model.user.User;
import org.bson.UuidRepresentation;
import org.mongojack.JacksonMongoCollection;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;

public class UserRepository {
    private final JacksonMongoCollection<User> collection;
    private final ExecutorService executor;

    public UserRepository(MongoManager mongoManager) {
        this.executor = mongoManager.getExecutor();
        this.collection = JacksonMongoCollection.builder()
                .withObjectMapper(mongoManager.getObjectMapper())
                .build(mongoManager.getDatabase(), "users", User.class, UuidRepresentation.STANDARD);


    }

    private void createIndexes() {
        collection.createIndex(Indexes.ascending("playerId"), new IndexOptions().unique(true));
        collection.createIndex(Indexes.compoundIndex(
                Indexes.descending("kills"),
                Indexes.ascending("deaths")
        ));
    }

    public CompletionStage<Optional<User>> findByPlayerId(UUID playerId) {
        return CompletableFuture.supplyAsync(
                () -> Optional.ofNullable(collection.find(Filters.eq("playerId", playerId)).first()), executor);
    }

    public CompletionStage<Void> save(User user) {
        return CompletableFuture.runAsync(() -> collection.replaceOne(
                Filters.eq("playerId", user.playerId()),
                user,
                new ReplaceOptions().upsert(true)
        ), executor);
    }

    public CompletionStage<Void> updatePower(UUID playerId, double powerDelta) {
        return CompletableFuture.runAsync(() -> collection.updateOne(
                Filters.eq("playerId", playerId),
                Updates.inc("power", powerDelta)
        ), executor);
    }

    public CompletionStage<Void> updateLastSeen(UUID playerId) {
        return CompletableFuture.runAsync(() -> collection.updateOne(
                Filters.eq("playerId", playerId),
                Updates.set("lastSeen", Instant.now())
        ), executor);
    }

    public CompletionStage<Void> incrementKills(UUID playerId) {
        return CompletableFuture.runAsync(() -> collection.updateOne(
                Filters.eq("playerId", playerId),
                Updates.inc("kills", 1)
        ), executor);
    }

    public CompletionStage<Void> incrementDeaths(UUID playerId) {
        return CompletableFuture.runAsync(() -> collection.updateOne(
                Filters.eq("playerId", playerId),
                Updates.inc("deaths", 1)
        ), executor);
    }

    public CompletionStage<List<User>> getTopKdrPlayers(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            FindIterable<User> findIterable = collection.find()
                    .sort(Sorts.orderBy(
                            Sorts.descending("kills"),
                            Sorts.ascending("deaths")
                    ))
                    .limit(limit);
            return findIterable.into(new ArrayList<>());
        }, executor);
    }
}
