package dev.balafini.factions.listener;

import dev.balafini.factions.scoreboard.ScoreboardManager;
import dev.balafini.factions.user.service.UserLifecycleService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

@SuppressWarnings("UnstableApiUsage")
public class PlayerJoinListener implements Listener {

    private final UserLifecycleService userLifecycleService;
    private final ScoreboardManager scoreboardManager;

    public PlayerJoinListener(UserLifecycleService userLifecycleService, ScoreboardManager scoreboardManager) {
        this.userLifecycleService = userLifecycleService;
        this.scoreboardManager = scoreboardManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        userLifecycleService.getOrCreateUser(player.getUniqueId(), player.getName())
                .exceptionally(ex -> {
                    player.sendMessage("§cOcorreu um erro ao carregar os seus dados de jogador.");
                    Bukkit.getLogger().warning("Failed to create/update user data for " + player.getName() + ": " + ex.getMessage());
                    return null;
                });

        scoreboardManager.addPlayer(player);
    }
}
