package dev.balafini.factions.listener;

import dev.balafini.factions.FactionsPlugin;
import dev.balafini.factions.scoreboard.ScoreboardManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

public class PlayerRespawnListener implements Listener {

    private final FactionsPlugin plugin;
    private final ScoreboardManager scoreboardManager;

    public PlayerRespawnListener(FactionsPlugin plugin, ScoreboardManager scoreboardManager) {
        this.plugin = plugin;
        this.scoreboardManager = scoreboardManager;
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        Bukkit.getScheduler().runTaskLater(plugin, () -> scoreboardManager.addPlayer(player), 1L);
    }
}
