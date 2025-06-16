package dev.balafini.factions;

import dev.balafini.factions.cache.FactionCache;
import dev.balafini.factions.command.FactionCommand;
import dev.balafini.factions.database.MongoConfig;
import dev.balafini.factions.database.MongoManager;
import dev.balafini.factions.repository.faction.FactionInviteRepository;
import dev.balafini.factions.repository.faction.FactionMemberRepository;
import dev.balafini.factions.repository.faction.FactionRepository;
import dev.balafini.factions.service.FactionInviteService;
import dev.balafini.factions.service.FactionService;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.incendo.cloud.SenderMapper;
import org.incendo.cloud.annotations.AnnotationParser;
import org.incendo.cloud.bukkit.CloudBukkitCapabilities;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.LegacyPaperCommandManager;

import java.util.concurrent.Executors;

public class FactionsPlugin extends JavaPlugin {

    private MongoManager mongoManager;

    private FactionRepository factionRepository;
    private FactionMemberRepository memberRepository;
    private FactionInviteRepository inviteRepository;

    private FactionCache factionCache;

    private FactionService factionService;
    private FactionInviteService inviteService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        int maxFactionSize = getConfig().getInt("factions.max-size", 15);

        this.setupDatabase();
        this.setupRepositories();
        this.setupCache();
        this.setupServices(maxFactionSize);

        registerCommands();
        registerListeners();

        getLogger().info("Factions plugin enabled successfully!");
    }

    @Override
    public void onDisable() {
        if (this.mongoManager != null) {
            this.mongoManager.close();
        }
        getLogger().info("Factions plugin disabled!");
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
    }

    private void setupCache() {
        this.factionCache = new FactionCache(
                this.factionRepository,
                this.memberRepository,
                this.mongoManager.getExecutor()
        );
    }

    private void setupServices(int maxFactionSize) {
        this.factionService = new FactionService(
                this.factionCache,
                this.factionRepository,
                this.memberRepository,
                maxFactionSize
        );
        this.inviteService = new FactionInviteService(
                this.inviteRepository,
                this.factionRepository,
                this.factionService
        );
    }


    private void registerCommands() {
        LegacyPaperCommandManager<CommandSender> commandManager = new LegacyPaperCommandManager<>(
                this,
                ExecutionCoordinator.coordinatorFor(Executors.newVirtualThreadPerTaskExecutor()),
                SenderMapper.identity()
        );

        if (commandManager.hasCapability(CloudBukkitCapabilities.NATIVE_BRIGADIER)) {
            commandManager.registerBrigadier();
        } else if (commandManager.hasCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {
            commandManager.registerAsynchronousCompletions();
        }

        AnnotationParser<CommandSender> annotationParser = new AnnotationParser<>(
                commandManager,
                CommandSender.class
        );

        annotationParser.parse(new FactionCommand(
                this,
                factionService,
                inviteService,
                this.mongoManager.getExecutor(),
                this.getConfig().getInt("factions.max-size", 15)));
    }


    private void registerListeners() {

    }

    public FactionService getFactionService() {
        return factionService;
    }

    public FactionCache getFactionCache() {
        return factionCache;
    }
}