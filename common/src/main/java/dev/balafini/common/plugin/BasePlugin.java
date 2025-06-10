package dev.balafini.common.plugin;

import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.incendo.cloud.Command;
import org.incendo.cloud.SenderMapper;
import org.incendo.cloud.annotations.AnnotationParser;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.LegacyPaperCommandManager;

import java.util.concurrent.Executors;

public class BasePlugin extends JavaPlugin {

    private LegacyPaperCommandManager<CommandSender> commandManager;
    private AnnotationParser<CommandSender> annotationParser;

    @Override
    public void onEnable() {
        super.onEnable();

        commandManager = new LegacyPaperCommandManager<>(
                this,
                ExecutionCoordinator.coordinatorFor(Executors.newVirtualThreadPerTaskExecutor()),
                SenderMapper.identity()
        );

        annotationParser = new AnnotationParser<>(commandManager, CommandSender.class);
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    public void parseCommands(Object... instances) {
        if (annotationParser != null) {
            annotationParser.parse(instances);
        }
    }

    public void registerCommands(Command<CommandSender> command) {
        if (commandManager != null) {
            commandManager.command(command);
        }
    }

    public void registerListener(Listener... listeners) {
        for (Listener listener : listeners) {
            getServer().getPluginManager().registerEvents(listener, this);
        }
    }
}
