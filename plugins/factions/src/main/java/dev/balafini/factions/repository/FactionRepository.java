package dev.balafini.factions.repository;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import dev.balafini.factions.database.MongoDBManager;
import dev.balafini.factions.model.Faction;
import dev.balafini.factions.model.FactionUser;
import org.bson.Document;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class FactionRepository {
    private final MongoDBManager mongoDBManager;
    private static final String FACTIONS_COLLECTION = "factions";
    private static final String FACTION_USERS_COLLECTION = "faction_users";

    public FactionRepository(MongoDBManager mongoDBManager) {
        this.mongoDBManager = mongoDBManager;
    }

    public CompletableFuture<Void> save(Faction faction) {
        return mongoDBManager.executeAsync(database -> {
            Document factionDoc = new Document()
                    .append("_id", faction.id().toString())
                    .append("name", faction.name())
                    .append("tag", faction.tag())
                    .append("createdAt", faction.createdAt().toEpochMilli());


            database.getCollection(FACTIONS_COLLECTION).replaceOne(
                    Filters.eq("_id", faction.id().toString()),
                    factionDoc,
                    new ReplaceOptions().upsert(true)
            );

            database.getCollection(FACTION_USERS_COLLECTION)
                    .deleteMany(Filters.eq("factionId", faction.id().toString()));

            List<Document> memberDocs = new ArrayList<>();
            for (FactionUser member : faction.members()) {
                Document memberDoc = new Document()
                        .append("_id", member.playerId().toString())
                        .append("factionId", member.factionId().toString())
                        .append("role", member.role().name())
                        .append("joinedAt", member.joinedAt().toEpochMilli());
                memberDocs.add(memberDoc);
            }
            if (!memberDocs.isEmpty()) {
                database.getCollection(FACTION_USERS_COLLECTION).insertMany(memberDocs);
            }
        });

    }

    public CompletableFuture<Optional<Faction>> findById(UUID id) {
        return mongoDBManager.executeAsync(database -> {
            Document factionDoc = database.getCollection(FACTIONS_COLLECTION)
                    .find(Filters.eq("_id", id.toString()))
                    .first();

            if (factionDoc == null) {
                return Optional.empty();
            }

            return Optional.of(buildFaction(factionDoc, database));
        });
    }

    public CompletableFuture<Optional<Faction>> findByName(String name) {
        return mongoDBManager.executeAsync(database -> {
            Document factionDoc = database.getCollection(FACTIONS_COLLECTION)
                    .find(Filters.regex("name", "^" + name + "$", "i"))
                    .first();

            if (factionDoc == null) {
                return Optional.empty();
            }

            return Optional.of(buildFaction(factionDoc, database));
        });
    }

    public CompletableFuture<Optional<Faction>> findByTag(String tag) {
        return mongoDBManager.executeAsync(database -> {
            Document factionDoc = database.getCollection(FACTIONS_COLLECTION)
                    .find(Filters.regex("tag", "^" + tag + "$", "i"))
                    .first();

            if (factionDoc == null) {
                return Optional.empty();
            }

            return Optional.of(buildFaction(factionDoc, database));
        });
    }

    public CompletableFuture<Optional<Faction>> findByPlayer(UUID playerId) {
        return mongoDBManager.executeAsync(database -> {
            Document memberDoc = database.getCollection(FACTION_USERS_COLLECTION)
                    .find(Filters.eq("_id", playerId.toString()))
                    .first();

            if (memberDoc == null) {
                return Optional.empty();
            }

            String factionId = memberDoc.getString("factionId");
            Document factionDoc = database.getCollection(FACTIONS_COLLECTION)
                    .find(Filters.eq("_id", factionId))
                    .first();

            if (factionDoc == null) {
                return Optional.empty();
            }

            return Optional.of(buildFaction(factionDoc, database));
        });
    }

    public CompletableFuture<Void> delete(UUID id) {
        return mongoDBManager.executeAsync(database -> {
            database.getCollection(FACTIONS_COLLECTION)
                    .deleteOne(Filters.eq("_id", id.toString()));

            database.getCollection(FACTION_USERS_COLLECTION)
                    .deleteMany(Filters.eq("factionId", id.toString()));
        });
    }

    public CompletableFuture<Boolean> existsByName(String name) {
        return mongoDBManager.executeAsync(database ->
                database.getCollection(FACTIONS_COLLECTION)
                        .find(Filters.regex("name", "^" + name + "$", "i"))
                        .first() != null
        );
    }

    public CompletableFuture<Boolean> existsByTag(String tag) {
        return mongoDBManager.executeAsync(database ->
                database.getCollection(FACTIONS_COLLECTION)
                        .find(Filters.regex("tag", "^" + tag + "$", "i"))
                        .first() != null
        );
    }

    private Faction buildFaction(Document factionDoc, MongoDatabase database) {
        UUID id = UUID.fromString(factionDoc.getString("_id"));
        String name = factionDoc.getString("name");
        String tag = factionDoc.getString("tag");
        Instant createdAt = Instant.ofEpochMilli(factionDoc.getLong("createdAt"));

        Set<FactionUser> members = new HashSet<>();

        database.getCollection(FACTION_USERS_COLLECTION)
                .find(Filters.eq("factionId", id.toString()))
                .forEach(memberDoc -> {
                    UUID playerId = UUID.fromString(memberDoc.getString("_id"));
                    String roleStr = memberDoc.getString("role");
                    Instant joinedAt = Instant.ofEpochMilli(memberDoc.getLong("joinedAt"));

                    FactionUser member = new FactionUser(
                            playerId, id,
                            FactionUser.FactionRole.valueOf(roleStr),
                            joinedAt
                    );
                    members.add(member);
                });

        return new Faction(id, name, tag, createdAt, members);
    }
}
