package dev.balafini.factions.faction.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import dev.balafini.factions.database.MongoManager;
import dev.balafini.factions.faction.FactionMember;
import org.bson.UuidRepresentation;
import org.mongojack.JacksonMongoCollection;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;

public class FactionMemberRepository {
    private final JacksonMongoCollection<FactionMember> collection;
    private final ExecutorService executor;

    public FactionMemberRepository(MongoManager mongoManager) {
        MongoDatabase database = mongoManager.getDatabase();
        ObjectMapper objectMapper = mongoManager.getObjectMapper();
        this.executor = mongoManager.getExecutor();
        this.collection = JacksonMongoCollection.builder()
                .withObjectMapper(objectMapper)
                .build(database, "faction_members", FactionMember.class, UuidRepresentation.STANDARD);

        createIndexes();
    }

    private void createIndexes() {
        collection.createIndex(Indexes.ascending("playerId"), new IndexOptions().unique(true));
        collection.createIndex(Indexes.ascending("factionId"));
    }

    public CompletionStage<Optional<FactionMember>> findByPlayerId(UUID playerId) {
        return CompletableFuture.supplyAsync(
                () -> Optional.ofNullable(collection.find(Filters.eq("playerId", playerId)).first()),
                executor
        );
    }

    public CompletionStage<Set<FactionMember>> findByFactionId(UUID factionId) {
        return CompletableFuture.supplyAsync(
                () -> collection.find(Filters.eq("factionId", factionId)).into(new HashSet<>()),
                executor
        );
    }

    public CompletionStage<Void> insert(FactionMember member) {
        return CompletableFuture.runAsync(() -> collection.insertOne(member), executor);
    }

    public CompletionStage<Void> insert(FactionMember member, ClientSession session) {
        return CompletableFuture.runAsync(() -> collection.insertOne(session, member), executor);
    }

    public CompletionStage<Void> update(FactionMember member) {
        return CompletableFuture.runAsync(() -> collection.replaceOne(
                Filters.eq("playerId", member.playerId()),
                member
        ), executor);
    }

    public CompletionStage<Boolean> deleteByPlayerId(UUID playerId) {
        return CompletableFuture.supplyAsync(
                () -> collection.deleteOne(Filters.eq("playerId", playerId)).getDeletedCount() > 0, executor);
    }

    public CompletionStage<Boolean> deleteByPlayerId(UUID playerId, ClientSession session) {
        return CompletableFuture.supplyAsync(
                () -> collection.deleteOne(
                        session,
                        Filters.eq("playerId", playerId)).getDeletedCount() > 0, executor);
    }

    public CompletionStage<Long> deleteByFactionId(UUID factionId) {
        return CompletableFuture.supplyAsync(
                () -> collection.deleteMany(Filters.eq("factionId", factionId)).getDeletedCount(), executor);
    }

    public CompletionStage<Long> deleteByFactionId(UUID factionId, ClientSession session) {
        return CompletableFuture.supplyAsync(
                () -> collection.deleteMany(
                        session,
                        Filters.eq("factionId", factionId)).getDeletedCount()
                , executor);
    }
}
