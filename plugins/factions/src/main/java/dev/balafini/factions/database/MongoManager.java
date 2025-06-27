package dev.balafini.factions.database;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mongodb.ConnectionString;
import com.mongodb.Function;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import lombok.Getter;
import org.bson.UuidRepresentation;
import org.mongojack.internal.MongoJackModule;

import java.util.concurrent.*;

@Getter
public class MongoManager implements AutoCloseable {

    private final MongoClient mongoClient;
    private final MongoDatabase database;
    private final ExecutorService executorService;
    private final ObjectMapper objectMapper;

    public MongoManager(MongoConfig config) {
        int corePoolSize = Runtime.getRuntime().availableProcessors();
        int maxPoolSize = corePoolSize * 4;
        MongoClientSettings settings = MongoClientSettings.builder()
            .applyConnectionString(new ConnectionString(config.connectionString()))
            .uuidRepresentation(UuidRepresentation.STANDARD)
            .applyToConnectionPoolSettings(builder ->
                builder.maxSize(maxPoolSize)
                    .minSize(corePoolSize)
                    .maxWaitTime(5000, TimeUnit.MILLISECONDS)
                    .maxConnectionIdleTime(30000, TimeUnit.MILLISECONDS))
            .build();

        this.mongoClient = MongoClients.create(settings);
        this.database = mongoClient.getDatabase(config.databaseName());
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
        this.objectMapper = createConfiguredObjectMapper();
    }

    private static ObjectMapper createConfiguredObjectMapper() {
        return new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .registerModule(new MongoJackModule())
            .configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true)
            .configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true)
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .configure(MapperFeature.AUTO_DETECT_SETTERS, false)
            .configure(MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS, true)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }


    public <T> CompletionStage<T> withTransaction(Function<ClientSession, CompletionStage<T>> action) {
        return CompletableFuture.supplyAsync(mongoClient::startSession, executorService)
            .thenCompose(session -> {
                session.startTransaction();
                return action.apply(session)
                    .thenCompose(result -> CompletableFuture.supplyAsync(() -> {
                        session.commitTransaction();
                        return result;
                    }, executorService))
                    .whenComplete((_, err) -> {
                        if (err != null && session.hasActiveTransaction()) {
                            session.abortTransaction();
                        }
                        session.close();
                    });
            });
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