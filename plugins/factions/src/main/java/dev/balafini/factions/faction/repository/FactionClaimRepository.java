package dev.balafini.factions.faction.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import dev.balafini.factions.database.MongoManager;
import dev.balafini.factions.faction.FactionClaim;
import dev.balafini.factions.faction.FactionInvite;
import org.bson.UuidRepresentation;
import org.bukkit.Chunk;
import org.mongojack.JacksonMongoCollection;
import org.mongojack.ObjectId;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;

public class FactionClaimRepository {

    private final JacksonMongoCollection<FactionClaim> collection;
    private final ExecutorService executor;

    public FactionClaimRepository(MongoManager mongoManager) {
        MongoDatabase database = mongoManager.getDatabase();
        ObjectMapper objectMapper = mongoManager.getObjectMapper();
        this.executor = mongoManager.getExecutor();
        this.collection = JacksonMongoCollection.builder()
                .withObjectMapper(objectMapper)
                .build(database, "faction_claims", FactionClaim.class, UuidRepresentation.STANDARD);

        createIndexes();
    }

    private void createIndexes() {
        collection.createIndex(Indexes.ascending("id"), new IndexOptions().unique(true));
    }

    public CompletionStage<Void> insert(FactionClaim claim) {
        return CompletableFuture.runAsync(() -> collection.insertOne(claim), executor);
    }

    public CompletionStage<Optional<FactionClaim>> findById(UUID uuid) {
        return CompletableFuture.supplyAsync(() ->
                Optional.ofNullable(collection.find(Filters.and(
                        Filters.eq("id", uuid)
                )).first()), executor);
    }

    public CompletionStage<Optional<FactionClaim>> findByChunk(Chunk chunk) {
        return CompletableFuture.supplyAsync(() ->
                Optional.ofNullable(collection.find(Filters.and(
                        Filters.eq("chunkX", chunk.getX()),
                        Filters.eq("chunkZ", chunk.getZ())
                )).first()), executor);
    }

    public CompletionStage<Boolean> deleteById(UUID uuid) {
        return CompletableFuture.supplyAsync(() ->
                collection.deleteOne(Filters.eq("id", uuid)).getDeletedCount() > 0, executor);
    }
}
