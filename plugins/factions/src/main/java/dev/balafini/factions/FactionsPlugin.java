package dev.balafini.factions;

import dev.balafini.factions.cache.FactionCache;
import dev.balafini.factions.cache.UserCache;
import dev.balafini.factions.command.FactionCommand;
import dev.balafini.factions.config.ConfigManager;
import dev.balafini.factions.database.MongoConfig;
import dev.balafini.factions.database.MongoManager;
import dev.balafini.factions.listener.PlayerDeathListener;
import dev.balafini.factions.listener.PlayerJoinListener;
import dev.balafini.factions.listener.PlayerQuitListener;
import dev.balafini.factions.repository.faction.FactionInviteRepository;
import dev.balafini.factions.repository.faction.FactionMemberRepository;
import dev.balafini.factions.repository.faction.FactionRepository;
import dev.balafini.factions.repository.user.UserRepository;
import dev.balafini.factions.scoreboard.ScoreboardManager;
import dev.balafini.factions.service.faction.FactionInviteService;
import dev.balafini.factions.service.faction.FactionService;
import dev.balafini.factions.service.user.UserService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.plugin.java.JavaPlugin;

public class FactionsPlugin extends JavaPlugin {

    private MongoManager mongoManager;
    private ConfigManager configManager;
    private ScoreboardManager scoreboardManager;

    private FactionRepository factionRepository;
    private FactionMemberRepository memberRepository;
    private FactionInviteRepository inviteRepository;
    private UserRepository userRepository;

    private FactionCache factionCache;
    private UserCache userCache;

    private FactionService factionService;
    private UserService userService;
    private FactionInviteService inviteService;

    @Override
    public void onEnable() {
        this.setupConfig();
        this.setupDatabase();
        this.setupRepositories();
        this.setupCaches();
        this.setupServices();
        this.setupScoreboard();
        this.registerCommands();
        this.registerListeners();

        getLogger().info("Factions plugin habilitado com sucesso!");
    }

    @Override
    public void onDisable() {
        if (this.scoreboardManager != null) {
            this.scoreboardManager.shutdown();
        }
        if (this.mongoManager != null) {
            this.mongoManager.close();
        }
        getLogger().info("Factions plugin disabled!");
    }

    private void setupConfig() {
        saveDefaultConfig();
        this.configManager = ConfigManager.load(this.getConfig());
    }

    private void setupDatabase() {
        MongoConfig mongoConfig = new MongoConfig(
                getConfig().getString("mongodb.host"),
                getConfig().getInt("mongodb.port"),
                getConfig().getString("mongodb.database"),
                getConfig().getString("mongodb.username"),
                getConfig().getString("mongodb.password"),
                getConfig().getBoolean("mongodb.useAuthentication")
        );

        this.mongoManager = new MongoManager(mongoConfig);
    }

    private void setupRepositories() {
        this.factionRepository = new FactionRepository(this.mongoManager);
        this.memberRepository = new FactionMemberRepository(this.mongoManager);
        this.inviteRepository = new FactionInviteRepository(this.mongoManager);
        this.userRepository = new UserRepository(this.mongoManager);
    }

    private void setupCaches() {
        this.factionCache = new FactionCache(this.factionRepository, this.memberRepository, this.mongoManager.getExecutor());
        this.userCache = new UserCache(this.userRepository, this.mongoManager.getExecutor());
    }

    private void setupServices() {
        this.userService = new UserService(this.userRepository, this.userCache, this.factionRepository, this.memberRepository, this.factionCache, this.configManager);
        this.factionService = new FactionService(this.factionCache, this.factionRepository, this.memberRepository, this.userService, this.configManager);
        this.inviteService = new FactionInviteService(this.inviteRepository, this.factionRepository, this.factionService);
    }

    private void setupScoreboard() {
        this.scoreboardManager = new ScoreboardManager(this, this.factionService, this.userService);
        Bukkit.getOnlinePlayers().forEach(scoreboardManager::addPlayer);
    }

    private void registerCommands() {
        CommandMap commandMap = Bukkit.getCommandMap();
        commandMap.register("clan", new FactionCommand(this, this.factionService, this.configManager, this.mongoManager.getExecutor()));
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this.userService, this.scoreboardManager), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this.userService, this.scoreboardManager), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this.userService), this);
    }


}