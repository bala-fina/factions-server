package dev.balafini.factions;

import dev.balafini.factions.faction.command.FactionCommand;
import dev.balafini.factions.faction.cache.FactionCache;
import dev.balafini.factions.config.ConfigManager;
import dev.balafini.factions.database.MongoConfig;
import dev.balafini.factions.database.MongoManager;
import dev.balafini.factions.faction.cache.FactionClaimCache;
import dev.balafini.factions.faction.cache.FactionMemberCache;
import dev.balafini.factions.faction.repository.FactionClaimRepository;
import dev.balafini.factions.faction.repository.FactionInviteRepository;
import dev.balafini.factions.faction.repository.FactionMemberRepository;
import dev.balafini.factions.faction.repository.FactionRepository;
import dev.balafini.factions.faction.service.*;
import dev.balafini.factions.faction.validator.FactionClaimValidator;
import dev.balafini.factions.faction.validator.FactionValidator;
import dev.balafini.factions.user.view.UserInvitePaginatedView;
import dev.balafini.factions.user.view.UserMainView;
import dev.balafini.factions.item.CustomItemRegistry;
import dev.balafini.factions.item.command.GiveCustomItemCommand;
import dev.balafini.factions.item.items.EnderPearlItem;
import dev.balafini.factions.item.items.FactionBannerItem;
import dev.balafini.factions.item.items.LightningMasterItem;
import dev.balafini.factions.item.items.PlayerTrackerItem;
import dev.balafini.factions.listener.PlayerDeathListener;
import dev.balafini.factions.listener.PlayerJoinListener;
import dev.balafini.factions.listener.PlayerQuitListener;
import dev.balafini.factions.listener.PlayerRespawnListener;
import dev.balafini.factions.scoreboard.ScoreboardManager;
import dev.balafini.factions.user.cache.UserCache;
import dev.balafini.factions.user.repository.UserRepository;
import dev.balafini.factions.user.service.UserCombatService;
import dev.balafini.factions.user.service.UserLifecycleService;
import dev.balafini.factions.user.service.UserPowerService;
import dev.balafini.factions.user.service.UserStatsService;
import dev.balafini.factions.util.CooldownUtil;
import lombok.Getter;
import me.saiintbrisson.minecraft.ViewFrame;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;
import java.util.logging.Logger;

@Getter
public class FactionsPlugin extends JavaPlugin {

    private MongoManager mongoManager;
    private ConfigManager configManager;
    private ScoreboardManager scoreboardManager;

    private FactionRepository factionRepository;
    private FactionMemberRepository factionMemberRepository;
    private FactionInviteRepository factionInviteRepository;
    private FactionClaimRepository factionClaimRepository;

    private UserRepository userRepository;

    private FactionCache factionCache;
    private FactionClaimCache factionClaimCache;
    private FactionMemberCache factionMemberCache;
    private UserCache userCache;

    private FactionValidator factionValidator;
    private FactionClaimValidator factionClaimValidator;

    private FactionClaimService factionClaimService;
    private FactionLifecycleService factionLifecycleService;
    private FactionMemberService factionMemberService;
    private FactionQueryService factionQueryService;
    private FactionStatsService factionStatsService;
    private FactionInviteService factionInviteService;

    private UserCombatService userCombatService;
    private UserLifecycleService userLifecycleService;
    private UserPowerService userPowerService;
    private UserStatsService userStatsService;

    private CooldownUtil cooldownUtil;
    private CustomItemRegistry customItemRegistry;

    private ViewFrame viewFrame;

    public static FactionsPlugin getInstance() {
        return getPlugin(FactionsPlugin.class);
    }

    @Override
    public void onEnable() {
        Logger.getLogger("org.mongodb.driver").setLevel(Level.WARNING);
        this.setupConfig();
        this.setupDatabase();
        this.setupRepositories();
        this.setupCaches();
        this.setupServices();
        this.setupScoreboard();
        this.registerViews();
        this.registerCommands();
        this.registerListeners();
        this.registerCustomItems();

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
        this.factionClaimRepository = new FactionClaimRepository(this.mongoManager);
    }

    private void setupCaches() {
        this.factionCache = new FactionCache(this.factionRepository, this.mongoManager.getExecutorService());
        this.factionMemberCache = new FactionMemberCache(this.factionMemberRepository, this.mongoManager.getExecutorService());
        this.userCache = new UserCache(this.userRepository, this.mongoManager.getExecutorService());
        this.factionClaimCache = new FactionClaimCache(this.factionClaimRepository, this.mongoManager.getExecutorService());
    }

    private void setupServices() {
        this.factionValidator = new FactionValidator(this.factionRepository, this.factionMemberRepository);
        this.factionClaimValidator = new FactionClaimValidator(this.factionClaimRepository);

        this.userLifecycleService = new UserLifecycleService(this.userCache, this.userRepository, this.configManager);
        this.userStatsService = new UserStatsService(this.userRepository, this.userCache);
        this.userCombatService = new UserCombatService(this.userLifecycleService, this.userStatsService, this.factionStatsService, this.configManager);
        this.userPowerService = new UserPowerService(this.userLifecycleService, this.userStatsService, this.factionStatsService, this.configManager);

        this.factionQueryService = new FactionQueryService(this.factionCache, this.factionRepository, this.factionMemberRepository);

        this.factionLifecycleService = new FactionLifecycleService(
            this.factionCache,
            this.factionRepository,
            this.factionMemberRepository,
            this.userLifecycleService,
            this.factionQueryService,
            this.factionValidator,
            this.mongoManager
        );

        this.factionMemberService = new FactionMemberService(
            new UserLifecycleService(this.userCache, this.userRepository, this.configManager),
            new FactionQueryService(this.factionCache, this.factionRepository, this.factionMemberRepository),
            this.factionMemberRepository,
            this.factionRepository,
            this.factionCache,
            this.factionMemberCache,
            this.mongoManager,
            this.configManager
        );

        this.factionStatsService = new FactionStatsService(this.factionRepository, this.factionMemberRepository, this.factionCache);
        this.factionInviteService = new FactionInviteService(this.factionInviteRepository, this.factionQueryService, this.factionMemberService);

        this.factionClaimService = new FactionClaimService(
            this.factionClaimValidator,
            this.factionClaimCache,
            this.factionClaimRepository,
            this.factionRepository,
            this.factionQueryService
        );
    }

    private void setupScoreboard() {
        this.scoreboardManager = new ScoreboardManager(this, this.factionQueryService, this.userLifecycleService);
        Bukkit.getOnlinePlayers().forEach(scoreboardManager::addPlayer);
    }

    private void registerViews() {
        viewFrame = ViewFrame.of(this).with(
                new UserMainView(this, this.factionInviteService),
                new UserInvitePaginatedView()
        ).register();
    }

    private void registerCustomItems() {
        this.cooldownUtil = new CooldownUtil();
        this.customItemRegistry = new CustomItemRegistry();

        customItemRegistry.register(new EnderPearlItem(this, this.cooldownUtil));
        customItemRegistry.register(new LightningMasterItem(this, this.cooldownUtil));
        customItemRegistry.register(new FactionBannerItem(this, this.cooldownUtil, factionQueryService));
        customItemRegistry.register(new PlayerTrackerItem(this, this.cooldownUtil));
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this.userLifecycleService, this.scoreboardManager), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this.userLifecycleService, this.scoreboardManager), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this, this.userCombatService), this);
        getServer().getPluginManager().registerEvents(new PlayerRespawnListener(this, this.scoreboardManager), this);
    }

    private void registerCommands() {
        CommandMap commandMap = Bukkit.getCommandMap();
        commandMap.register("factions", new FactionCommand(this, factionQueryService, userLifecycleService, viewFrame));
        commandMap.register("givecustomitem", new GiveCustomItemCommand(customItemRegistry));
    }
}