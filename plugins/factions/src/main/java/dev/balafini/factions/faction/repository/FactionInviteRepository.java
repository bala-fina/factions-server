package dev.balafini.factions.faction.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import dev.balafini.factions.database.MongoManager;
import dev.balafini.factions.faction.FactionInvite;
import org.bson.UuidRepresentation;
import org.mongojack.JacksonMongoCollection;
import org.mongojack.ObjectId;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;

public class FactionInviteRepository {

    private final JacksonMongoCollection<FactionInvite> collection;
    private final ExecutorService executor;

    public FactionInviteRepository(MongoManager mongoManager) {
        MongoDatabase database = mongoManager.getDatabase();
        ObjectMapper objectMapper = mongoManager.getObjectMapper();
        this.executor = mongoManager.getExecutorService();
        this.collection = JacksonMongoCollection.builder()
                .withObjectMapper(objectMapper)
                .build(database, "faction_invites", FactionInvite.class, UuidRepresentation.STANDARD);

        createIndexes();
    }

    private void createIndexes() {
        collection.createIndex(Indexes.compoundIndex(
                Indexes.ascending("inviteeId"),
                Indexes.ascending("factionTag")
        ), new IndexOptions().unique(true));
    }

    public CompletionStage<Void> insert(FactionInvite invite) {
        return CompletableFuture.runAsync(() -> collection.insertOne(invite), executor);
    }

    public CompletionStage<Optional<FactionInvite>> findByInviteeAndTag(UUID inviteeId, String factionTag) {
        return CompletableFuture.supplyAsync(() ->
                Optional.ofNullable(collection.find(Filters.and(
                        Filters.eq("inviteeId", inviteeId),
                        Filters.eq("factionTag", factionTag)
                )).first()), executor);
    }

    public CompletionStage<List<FactionInvite>> findAllByInviteeId(UUID inviteeId) {
        return CompletableFuture.supplyAsync(() ->
                collection.find(Filters.eq("inviteeId", inviteeId)).into(new ArrayList<>()), executor);
    }

    public CompletionStage<Long> deleteByInviteeId(UUID inviteeId) {
        return CompletableFuture.supplyAsync(() ->
                collection.deleteMany(Filters.eq("inviteeId", inviteeId)).getDeletedCount(), executor);
    }

    public CompletionStage<Boolean> deleteById(@ObjectId String id) {
        return CompletableFuture.supplyAsync(() ->
                collection.deleteOne(Filters.eq("_id", new org.bson.types.ObjectId(id))).getDeletedCount() > 0, executor);
    }
}
