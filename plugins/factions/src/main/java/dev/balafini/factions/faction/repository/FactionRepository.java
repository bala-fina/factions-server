package dev.balafini.factions.faction.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.ClientSession;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import dev.balafini.factions.database.MongoManager;
import dev.balafini.factions.faction.Faction;
import org.bson.UuidRepresentation;
import org.mongojack.JacksonMongoCollection;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Aggregates.lookup;
import static com.mongodb.client.model.Aggregates.match;

public class FactionRepository {

    private final JacksonMongoCollection<Faction> collection;
    private final ExecutorService executor;

    public FactionRepository(MongoManager mongoManager) {
        MongoDatabase database = mongoManager.getDatabase();
        ObjectMapper objectMapper = mongoManager.getObjectMapper();
        this.executor = mongoManager.getExecutorService();
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

    // find faction with memberIds
    public CompletableFuture<Optional<Faction>> findFactionById(UUID factionId) {
        return CompletableFuture.supplyAsync(() -> Optional.ofNullable(
            collection.aggregate(List.of(
                    match(Filters.eq("factionId", factionId))
            )).first()
        ), executor);
    }

    public CompletableFuture<Optional<Faction>> findFactionByName(String name) {
        return CompletableFuture.supplyAsync(() -> Optional.ofNullable(
            collection.aggregate(List.of(
                    match(Filters.regex("name", "^" + Pattern.quote(name) + "$", "i"))
            )).first()
        ), executor);
    }

    public CompletableFuture<Optional<Faction>> findFactionByTag(String tag) {
        return CompletableFuture.supplyAsync(() -> Optional.ofNullable(
            collection.aggregate(List.of(
                    match(Filters.regex("tag", "^" + Pattern.quote(tag) + "$", "i"))
            )).first()
        ), executor);
    }

    public CompletableFuture<Boolean> existsByTag(String tag) {
        Pattern pattern = Pattern.compile("^" + Pattern.quote(tag) + "$", Pattern.CASE_INSENSITIVE);
        return CompletableFuture.supplyAsync(
            () -> collection.countDocuments(Filters.regex("tag", pattern)) > 0,
            executor
        );
    }

    public CompletableFuture<Boolean> existsByName(String name) {
        Pattern pattern = Pattern.compile("^" + Pattern.quote(name) + "$", Pattern.CASE_INSENSITIVE);
        return CompletableFuture.supplyAsync(
            () -> collection.countDocuments(Filters.regex("name", pattern)) > 0,
            executor
        );
    }

    public CompletableFuture<Faction> upsert(Faction faction, ClientSession session) {
        return CompletableFuture.supplyAsync(() ->
            collection.findOneAndReplace(session, Filters.eq("factionId", faction.factionId()), faction, new FindOneAndReplaceOptions().upsert(true)), executor);
    }

    public CompletableFuture<Faction> upsert(Faction faction) {
        return CompletableFuture.supplyAsync(
            () -> collection.findOneAndReplace(Filters.eq("factionId", faction.factionId()), faction, new FindOneAndReplaceOptions().upsert(true)), executor);
    }

    public CompletableFuture<Boolean> deleteByFactionId(UUID factionId) {
        return CompletableFuture.supplyAsync(() ->
            collection.deleteOne(Filters.eq("factionId", factionId)).getDeletedCount() > 0, executor);
    }

    public CompletableFuture<Boolean> deleteByFactionId(UUID factionId, ClientSession session) {
        return CompletableFuture.supplyAsync(() ->
            collection.deleteOne(session, Filters.eq("factionId", factionId)).getDeletedCount() > 0, executor);
    }

    public CompletableFuture<Void> updatePower(UUID factionId, double powerDelta, double maxPowerDelta) {
        return CompletableFuture.runAsync(
            () -> collection.updateOne(
                Filters.eq("factionId", factionId),
                Updates.combine(
                    Updates.inc("power", powerDelta),
                    Updates.inc("maxPower", maxPowerDelta)
                )
            ), executor);
    }

    public CompletableFuture<Void> updatePower(UUID factionId, double powerDelta, double maxPowerDelta, ClientSession session) {
        return CompletableFuture.runAsync(() ->
            collection.updateOne(
                session,
                Filters.eq("factionId", factionId),
                Updates.combine(
                    Updates.inc("power", powerDelta),
                    Updates.inc("maxPower", maxPowerDelta)
                )
            ), executor);
    }

    public CompletableFuture<Void> updateKdr(UUID factionId, double kdrDelta) {
        return CompletableFuture.runAsync(() ->
            collection.updateOne(
                Filters.eq("factionId", factionId),
                Updates.inc("kdr", kdrDelta)
            ), executor);
    }

    public CompletableFuture<Long> countFactionsWithHigherKdr(double kdr) {
        return CompletableFuture.supplyAsync(() ->
            collection.countDocuments(Filters.gt("kdr", kdr)), executor);
    }

    public CompletableFuture<List<Faction>> getTopKdrFactions(int limit) {
        return CompletableFuture.supplyAsync(
            () -> {
                FindIterable<Faction> findIterable = collection.find()
                    .sort(Sorts.descending("kdr"))
                    .limit(limit);
                return findIterable.into(new ArrayList<>());
            }, executor
        );
    }
}

