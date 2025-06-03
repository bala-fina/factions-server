package dev.balafini.factions;

import dev.balafini.factions.cache.FactionCache;
import dev.balafini.factions.command.FactionCommand;
import dev.balafini.factions.service.FactionService;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class FactionsPlugin extends JavaPlugin {

    private FactionCache cache;
    private FactionService factionService;

    @Override
    public void onEnable() {
        this.cache = new FactionCache();
        this.factionService = new FactionService(cache);

        registerCommands();

        getLogger().info("Factions plugin enabled successfully!");
    }

    @Override
    public void onDisable() {
        if (cache != null) {
            cache.clear();
        }

        getLogger().info("Faction plugin disabled!");

    }

    private void registerCommands() {

        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands commands = event.registrar();
            commands.register(
                    "f",
                    List.of("faction", "fac"),
                    new FactionCommand(factionService)
            );
        });
    }
}