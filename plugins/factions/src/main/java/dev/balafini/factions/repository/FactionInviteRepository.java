package dev.balafini.factions.repository;

import com.mongodb.client.model.Filters;
import dev.balafini.factions.database.MongoDBManager;
import dev.balafini.factions.model.FactionInvite;
import org.bson.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class FactionInviteRepository {
    private final MongoDBManager mongoDBManager;
    private static final String INVITE_COLLECTION = "faction_invites";

    public FactionInviteRepository(MongoDBManager mongoDBManager) {
        this.mongoDBManager = mongoDBManager;
    }

    public CompletableFuture<Void> save(FactionInvite invite) {
        return mongoDBManager.executeAsync(database -> {
            Document inviteDoc = new Document()
                    .append("_id", invite.inviteId().toString())
                    .append("factionId", invite.factionId().toString())
                    .append("inviterId", invite.inviterId().toString())
                    .append("inviteeId", invite.inviteeId().toString())
                    .append("createdAt", invite.createdAt().toEpochMilli());

            database.getCollection(INVITE_COLLECTION).replaceOne(
                    Filters.eq("_id", invite.inviteId().toString()),
                    inviteDoc,
                    new com.mongodb.client.model.ReplaceOptions().upsert(true)
            );
        });
    }

    public CompletableFuture<Optional<FactionInvite>> findById(UUID inviteId) {
        return mongoDBManager.executeAsync(database -> {
            Document inviteDoc = database.getCollection(INVITE_COLLECTION)
                    .find(Filters.eq("_id", inviteId.toString()))
                    .first();

            if (inviteDoc == null) {
                return Optional.empty();
            }

            return Optional.of(buildInviteFromDocument(inviteDoc));
        });
    }

    public CompletableFuture<Optional<FactionInvite>> findByPlayer(UUID playerId) {
        return mongoDBManager.executeAsync(database -> {
            Document inviteDoc = database.getCollection(INVITE_COLLECTION)
                    .find(Filters.eq("inviteeId", playerId.toString()))
                    .first();

            if (inviteDoc == null) {
                return Optional.empty();
            }

            return Optional.of(buildInviteFromDocument(inviteDoc));
        });
    }

    public CompletableFuture<List<FactionInvite>> findByFaction(UUID factionId) {
        return mongoDBManager.executeAsync(database -> {
            List<FactionInvite> invites = new ArrayList<>();

            database.getCollection(INVITE_COLLECTION)
                    .find(Filters.eq("factionId", factionId.toString()))
                    .forEach(doc -> invites.add(buildInviteFromDocument(doc)));

            return invites;
        });
    }

    public CompletableFuture<Void> delete(UUID inviteId) {
        return mongoDBManager.executeAsync(database -> {
            database.getCollection(INVITE_COLLECTION)
                    .deleteOne(Filters.eq("_id", inviteId.toString()));
        });
    }

    public CompletableFuture<Boolean> hasActiveInvite(UUID playerId) {
        return mongoDBManager.executeAsync(database ->
                database.getCollection(INVITE_COLLECTION)
                        .find(Filters.eq("inviteeId", playerId.toString()))
                        .first() != null
        );
    }

    public CompletableFuture<Void> saveAll(List<FactionInvite> invites) {
        if (invites.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        return mongoDBManager.executeAsync(database -> {
            List<Document> inviteDocs = new ArrayList<>();

            for (FactionInvite invite : invites) {
                Document inviteDoc = new Document()
                        .append("_id", invite.inviteId().toString())
                        .append("factionId", invite.factionId().toString())
                        .append("inviterId", invite.inviterId().toString())
                        .append("inviteeId", invite.inviteeId().toString())
                        .append("createdAt", invite.createdAt().toEpochMilli());

                inviteDocs.add(inviteDoc);
            }

            database.getCollection(INVITE_COLLECTION).insertMany(inviteDocs);
        });
    }

    public CompletableFuture<Integer> deleteAll(List<UUID> inviteIds) {
        if (inviteIds.isEmpty()) {
            return CompletableFuture.completedFuture(0);
        }

        return mongoDBManager.executeAsync(database -> {
            List<String> stringIds = inviteIds.stream()
                    .map(UUID::toString)
                    .toList();

            com.mongodb.client.result.DeleteResult result =
                    database.getCollection(INVITE_COLLECTION)
                            .deleteMany(Filters.in("_id", stringIds));

            return (int) result.getDeletedCount();
        });
    }

    public CompletableFuture<List<FactionInvite>> findAllForPlayer(UUID playerId) {
        return mongoDBManager.executeAsync(database -> {
            List<FactionInvite> invites = new ArrayList<>();
            String playerIdStr = playerId.toString();

            database.getCollection(INVITE_COLLECTION)
                    .find(Filters.or(
                            Filters.eq("inviterId", playerIdStr),
                            Filters.eq("inviteeId", playerIdStr)
                    ))
                    .forEach(doc -> invites.add(buildInviteFromDocument(doc)));

            return invites;
        });
    }

    private FactionInvite buildInviteFromDocument(Document doc) {
        return new FactionInvite(
                UUID.fromString(doc.getString("_id")),
                UUID.fromString(doc.getString("factionId")),
                UUID.fromString(doc.getString("inviterId")),
                UUID.fromString(doc.getString("inviteeId")),
                Instant.ofEpochMilli(doc.getLong("createdAt"))
        );
    }
}
