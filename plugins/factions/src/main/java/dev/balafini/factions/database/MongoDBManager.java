package dev.balafini.factions.database;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

public class MongoDBManager implements AutoCloseable {

    private final MongoClient mongoClient;
    private final MongoDatabase database;
    private final ExecutorService executorService;

    public MongoDBManager(MongoDBConfig config) {
        CodecRegistry pojoCodecRegistry = CodecRegistries.fromRegistries(
                MongoClientSettings.getDefaultCodecRegistry(),
                CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build())
        );

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(config.getConnectionString()))
                .codecRegistry(pojoCodecRegistry)
                .build();

        this.mongoClient = MongoClients.create(settings);
        this.database = mongoClient.getDatabase(config.getDatabase());
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();

        setupCollections();
    }

    private void setupCollections() {
        if (isCollectionMissing("factions")) {
            database.createCollection("factions");
            database.getCollection("factions").createIndex(
                    new Document("name", 1),
                    new IndexOptions().unique(true)
            );
            database.getCollection("factions").createIndex(
                    new Document("tag", 1),
                    new IndexOptions().unique(true)
            );
        }
        if (isCollectionMissing("faction_users")) {
            database.createCollection("faction_users");
            database.getCollection("faction_users").createIndex(
                    new Document("playerId", 1),
                    new IndexOptions().unique(true)
            );
            database.getCollection("faction_users").createIndex(
                    new Document("factionId", 1)
            );
        }
        if (isCollectionMissing("faction_invites")) {
            database.createCollection("faction_invites");
            database.getCollection("faction_invites").createIndex(
                    new Document("inviteeId", 1),
                    new IndexOptions().unique(true)
            );
            database.getCollection("faction_invites").createIndex(
                    new Document("factionId", 1)
            );
        }
    }

    private boolean isCollectionMissing(String collectionName) {
        for (String name : database.listCollectionNames()) {
            if (name.equals(collectionName)) {
                return false;
            }
        }
        return true;
    }

    public MongoCollection<Document> getCollection(String collectionName) {
        return database.getCollection(collectionName);
    }

    public <T> MongoCollection<T> getCollection(String collectionName, Class<T> documentClass) {
        return database.getCollection(collectionName, documentClass);
    }

    public <T> CompletableFuture<T> executeAsync(Function<MongoDatabase, T> operation) {
        return CompletableFuture.supplyAsync(() -> operation.apply(database), executorService);
    }

    public CompletableFuture<Void> executeAsync(MongoOperation operation) {
        return CompletableFuture.runAsync(() -> operation.execute(database), executorService);
    }

    @Override
    public void close() {
        mongoClient.close();
        executorService.shutdown();
    }

    @FunctionalInterface
    public interface MongoOperation {
        void execute(MongoDatabase database);
    }
}