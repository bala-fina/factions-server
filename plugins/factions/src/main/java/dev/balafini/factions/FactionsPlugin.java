package dev.balafini.factions;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import dev.balafini.common.plugin.BasePlugin;
import dev.balafini.factions.cache.FactionCache;
import dev.balafini.factions.cache.FactionInviteCache;
import dev.balafini.factions.command.FactionCommand;
import dev.balafini.factions.database.MongoDBConfig;
import dev.balafini.factions.database.MongoDBManager;
import dev.balafini.factions.repository.FactionInviteRepository;
import dev.balafini.factions.repository.FactionRepository;
import dev.balafini.factions.service.FactionInviteService;
import dev.balafini.factions.service.FactionService;
import org.bukkit.configuration.file.FileConfiguration;

public class FactionsPlugin extends BasePlugin {

    private MongoDBManager mongoDBManager;

    private FactionCache factionCache;
    private FactionInviteCache inviteCache;
    private FactionRepository factionRepository;
    private FactionInviteRepository inviteRepository;
    private FactionService factionService;
    private FactionInviteService inviteService;

    @Override
    public void onEnable() {
        super.onEnable();

        saveDefaultConfig();

        initializeMongo();
        initializeServices();
        registerCommands();
        registerListeners();

        getLogger().info("Factions plugin enabled successfully!");
    }

    @Override
    public void onDisable() {
        if (mongoDBManager != null) {
            mongoDBManager.close();
        }

        getLogger().info("Faction plugin disabled!");

    }

    private void initializeMongo() {
        try {
            FileConfiguration config = getConfig();
            MongoDBConfig mongoConfig = new MongoDBConfig(
                    config.getString("mongodb.host"),
                    config.getInt("mongodb.port"),
                    config.getString("mongodb.database"),
                    config.getString("mongodb.username"),
                    config.getString("mongodb.password"),
                    config.getBoolean("mongodb.auth-enabled"),
                    config.getBoolean("mongodb.is-atlas")
            );

            mongoDBManager = new MongoDBManager(mongoConfig);
            getLogger().info("Connected to MongoDB database successfully!");
        } catch (Exception e) {
            getLogger().severe("Failed to initialize MongoDB: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void initializeServices() {
        factionCache = new FactionCache();
        inviteCache = new FactionInviteCache();

        factionRepository = new FactionRepository(mongoDBManager);
        inviteRepository = new FactionInviteRepository(mongoDBManager);

        factionService = new FactionService(factionCache, factionRepository);
        inviteService = new FactionInviteService(inviteCache, inviteRepository, factionService);
    }

    private void registerCommands() {
        parseCommands(FactionCommand.class);
    }

    private void registerListeners() {

    }

    public FactionCache getFactionCache() {
        return factionCache;
    }

    public FactionInviteCache getInviteCache() {
        return inviteCache;
    }

    public FactionRepository getFactionRepository() {
        return factionRepository;
    }

    public FactionInviteRepository getInviteRepository() {
        return inviteRepository;
    }
}