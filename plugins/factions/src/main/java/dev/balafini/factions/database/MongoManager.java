package dev.balafini.factions.database;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import dev.balafini.factions.model.faction.Faction;
import dev.balafini.factions.model.faction.FactionInvite;
import dev.balafini.factions.model.faction.FactionMember;
import org.bson.UuidRepresentation;
import org.mongojack.JacksonMongoCollection;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class MongoManager implements AutoCloseable {

    private final MongoClient mongoClient;
    private final MongoDatabase database;
    private final ExecutorService executorService;
    private final ObjectMapper objectMapper; // <-- NEW FIELD

    public MongoManager(MongoConfig config) {
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString("mongodb+srv://totoadmin:T33mPpyEF05powAv@factionsdevcluster.oahzqfh.mongodb.net/?retryWrites=true&w=majority&appName=factionsdevcluster"))
                .uuidRepresentation(UuidRepresentation.STANDARD)
                .applyToConnectionPoolSettings(builder ->
                        builder.maxSize(10)
                                .maxWaitTime(10000, TimeUnit.MILLISECONDS))
                .applyToSocketSettings(builder ->
                        builder.connectTimeout(10000, TimeUnit.MILLISECONDS)
                                .readTimeout(10000, TimeUnit.MILLISECONDS))
                .build();

        this.mongoClient = MongoClients.create(settings);
        this.database = mongoClient.getDatabase("factionsdev");
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
        this.objectMapper = createConfiguredObjectMapper();
    }

    private static ObjectMapper createConfiguredObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        return objectMapper;
    }

    public MongoDatabase getDatabase() {
        return database;
    }

    public ExecutorService getExecutor() {
        return executorService;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }


    @Override
    public void close() {
        mongoClient.close();
        try {
            executorService.shutdown();
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executorService.shutdownNow();
        }
    }
}