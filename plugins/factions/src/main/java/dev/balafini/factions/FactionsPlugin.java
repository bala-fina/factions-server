package dev.balafini.factions;

import dev.balafini.factions.cache.FactionCache;
import dev.balafini.factions.cache.UserCache;
import dev.balafini.factions.command.CommandExceptionHandler;
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
import dev.balafini.factions.service.faction.*;
import dev.balafini.factions.service.user.UserCombatService;
import dev.balafini.factions.service.user.UserLifecycleService;
import dev.balafini.factions.service.user.UserPowerService;
import dev.balafini.factions.service.user.UserStatsService;
import dev.balafini.factions.util.FactionValidator;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.incendo.cloud.annotations.AnnotationParser;
import org.incendo.cloud.bukkit.parser.PlayerParser;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.LegacyPaperCommandManager;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.incendo.cloud.bukkit.CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION;
import static org.incendo.cloud.bukkit.CloudBukkitCapabilities.NATIVE_BRIGADIER;

public class FactionsPlugin extends JavaPlugin {


    private MongoManager mongoManager;
    private ConfigManager configManager;
    private ScoreboardManager scoreboardManager;

    private FactionRepository factionRepository;
    private FactionMemberRepository factionMemberRepository;
    private FactionInviteRepository factionInviteRepository;
    private UserRepository userRepository;

    private FactionCache factionCache;
    private UserCache userCache;

    private FactionValidator factionValidator;
    private FactionLifecycleService factionLifecycleService;
    private FactionMembershipService factionMembershipService;
    private FactionQueryService factionQueryService;
    private FactionStatsService factionStatsService;
    private FactionInviteService factionInviteService;

    private UserCombatService userCombatService;
    private UserLifecycleService userLifecycleService;
    private UserPowerService userPowerService;
    private UserStatsService userStatsService;

    @Override
    public void onEnable() {
        Logger.getLogger("org.mongodb.driver").setLevel(Level.WARNING);
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
        MongoConfig mongoConfig = MongoConfig.load(this.getConfig());
        this.mongoManager = new MongoManager(mongoConfig);
    }

    private void setupRepositories() {
        this.factionRepository = new FactionRepository(this.mongoManager);
        this.factionMemberRepository = new FactionMemberRepository(this.mongoManager);
        this.factionInviteRepository = new FactionInviteRepository(this.mongoManager);
        this.userRepository = new UserRepository(this.mongoManager);
    }

    private void setupCaches() {
        this.factionCache = new FactionCache(this.factionRepository, this.mongoManager.getExecutor());
        this.userCache = new UserCache(this.userRepository, this.mongoManager.getExecutor());
    }

    private void setupServices() {
        this.factionValidator = new FactionValidator(this.factionRepository, this.factionMemberRepository);
        this.factionStatsService = new FactionStatsService(this.factionRepository, this.factionMemberRepository, this.factionCache);
        this.userStatsService = new UserStatsService(this.userRepository, this.userCache);

        this.factionQueryService = new FactionQueryService(this.factionCache, this.factionRepository, this.factionMemberRepository);
        this.userLifecycleService = new UserLifecycleService(this.userCache, this.userRepository, this.configManager);

        this.userCombatService = new UserCombatService(this.userLifecycleService, this.userStatsService, this.factionStatsService, this.configManager);
        this.userPowerService = new UserPowerService(this.userLifecycleService, this.userStatsService, this.factionStatsService, this.configManager);
        this.factionMembershipService = new FactionMembershipService(this.userLifecycleService, this.factionQueryService, this.factionMemberRepository, this.factionRepository, this.mongoManager, this.factionCache, this.configManager);
        this.factionInviteService = new FactionInviteService(this.factionInviteRepository, this.factionQueryService, this.factionMembershipService);
        this.factionLifecycleService = new FactionLifecycleService(this.factionCache, this.factionRepository, this.factionMemberRepository, this.userLifecycleService, this.factionQueryService, this.factionValidator, this.mongoManager);
    }

    private void setupScoreboard() {
        this.scoreboardManager = new ScoreboardManager(this, this.factionQueryService, this.userLifecycleService);
        Bukkit.getOnlinePlayers().forEach(scoreboardManager::addPlayer);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this.userLifecycleService, this.scoreboardManager), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this.userLifecycleService, this.scoreboardManager), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this.userCombatService), this);
    }

    private void registerCommands() {
        try {
            LegacyPaperCommandManager<CommandSender> commandManager = LegacyPaperCommandManager.createNative(
                    this,
                    ExecutionCoordinator.asyncCoordinator());

            AnnotationParser<CommandSender> annotationParser = new AnnotationParser<>(
                    commandManager,
                    CommandSender.class
                    );

            if (commandManager.hasCapability(NATIVE_BRIGADIER)) {
                commandManager.registerBrigadier();
            } else if (commandManager.hasCapability(ASYNCHRONOUS_COMPLETION)) {
                commandManager.registerAsynchronousCompletions();
            }
            
            annotationParser.parse(new CommandExceptionHandler());
            annotationParser.parse(new FactionCommand(this,
                    this.factionLifecycleService,
                    this.factionMembershipService,
                    this.factionInviteService,
                    this.factionQueryService));


        } catch (Exception e) {
            getLogger().severe("Erro ao registrar comandos: " + e.getMessage());
        }
    }


}