package dev.balafini.factions.repository.faction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import dev.balafini.factions.database.MongoManager;
import dev.balafini.factions.model.faction.Faction;
import org.bson.UuidRepresentation;
import org.mongojack.JacksonMongoCollection;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;

public class FactionRepository {

    private final JacksonMongoCollection<Faction> collection;
    private final ExecutorService executor;

    public FactionRepository(MongoManager mongoManager) {
        MongoDatabase database = mongoManager.getDatabase();
        ObjectMapper objectMapper = mongoManager.getObjectMapper();
        this.executor = mongoManager.getExecutor();
        this.collection = JacksonMongoCollection.builder()
                .withObjectMapper(objectMapper)
                .build(database, "factions", Faction.class, UuidRepresentation.STANDARD);

        createIndexes();
    }

    private void createIndexes() {
        IndexOptions caseInsensitiveUnique = new IndexOptions().unique(true).collation(
                Collation.builder()
                        .locale("en")
                        .collationStrength(CollationStrength.SECONDARY)
                        .build()
        );
        collection.createIndex(Indexes.ascending("name"), caseInsensitiveUnique);
        collection.createIndex(Indexes.ascending("tag"), caseInsensitiveUnique);
        collection.createIndex(Indexes.descending("kdr"));
    }

    public CompletionStage<Optional<Faction>> findById(UUID factionId) {
        return CompletableFuture.supplyAsync(
                () -> Optional.ofNullable(collection.find(Filters.eq("_id", factionId)).first()),
                executor
        );
    }

    public CompletionStage<Optional<Faction>> findByName(String name) {
        Pattern pattern = Pattern.compile("^" + Pattern.quote(name) + "$", Pattern.CASE_INSENSITIVE);
        return CompletableFuture.supplyAsync(
                () -> Optional.ofNullable(collection.find(Filters.regex("name", pattern)).first()),
                executor
        );
    }

    public CompletionStage<Optional<Faction>> findByTag(String tag) {
        Pattern pattern = Pattern.compile("^" + Pattern.quote(tag) + "$", Pattern.CASE_INSENSITIVE);
        return CompletableFuture.supplyAsync(
                () -> Optional.ofNullable(collection.find(Filters.regex("tag", pattern)).first()),
                executor
        );
    }

    public CompletionStage<Boolean> existsByName(String name) {
        Pattern pattern = Pattern.compile("^" + Pattern.quote(name) + "$", Pattern.CASE_INSENSITIVE);
        return CompletableFuture.supplyAsync(
                () -> collection.countDocuments(Filters.regex("name", pattern)) > 0,
                executor
        );
    }

    public CompletionStage<Boolean> existsByTag(String tag) {
        Pattern pattern = Pattern.compile("^" + Pattern.quote(tag) + "$", Pattern.CASE_INSENSITIVE);
        return CompletableFuture.supplyAsync(
                () -> collection.countDocuments(Filters.regex("tag", pattern)) > 0,
                executor
        );
    }

    public CompletionStage<Void> save(Faction faction) {
        return CompletableFuture.runAsync(() -> collection.replaceOne(
                Filters.eq("_id", faction.factionId()),
                faction,
                new ReplaceOptions().upsert(true)
        ), executor);
    }

    public CompletionStage<Boolean> deleteByFactionId(UUID factionId) {
        return CompletableFuture.supplyAsync(
                () -> collection.deleteOne(Filters.eq("_id", factionId)).getDeletedCount() > 0,
                executor
        );
    }

    public CompletionStage<Void> updatePower(UUID factionId, double powerDelta, double maxPowerDelta) {
        return CompletableFuture.runAsync(() -> collection.updateOne(
                Filters.eq("_id", factionId),
                Updates.combine(
                        Updates.inc("power", powerDelta),
                        Updates.inc("maxPower", maxPowerDelta)
                )
        ), executor);
    }

    public CompletionStage<Void> updateKdr(UUID factionId, double kdrDelta) {
        return CompletableFuture.runAsync(() -> collection.updateOne(
                Filters.eq("_id", factionId),
                Updates.inc("kdr", kdrDelta)
        ), executor);
    }

    public CompletionStage<Long> countFactionsWithHigherKdr(double kdr) {
        return CompletableFuture.supplyAsync(
                () -> collection.countDocuments(Filters.gt("kdr", kdr)),
                executor
        );
    }

    public CompletionStage<List<Faction>> getTopKdrFactions(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            FindIterable<Faction> findIterable = collection.find()
                    .sort(Sorts.descending("kdr"))
                    .limit(limit);
            return findIterable.into(new ArrayList<>());
        }, executor);
    }
}

