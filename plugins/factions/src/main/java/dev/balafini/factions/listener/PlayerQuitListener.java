package dev.balafini.factions.listener;

import dev.balafini.factions.scoreboard.ScoreboardManager;
import dev.balafini.factions.user.service.UserLifecycleService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

@SuppressWarnings("UnstableApiUsage")
public class PlayerQuitListener implements Listener {

    private final UserLifecycleService userLifecycleService;
    private final ScoreboardManager scoreboardManager;

    public PlayerQuitListener(UserLifecycleService userLifecycleService, ScoreboardManager scoreboardManager) {
        this.userLifecycleService = userLifecycleService;
        this.scoreboardManager = scoreboardManager;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        event.quitMessage(null);
        Player player = event.getPlayer();
        userLifecycleService.updateUserLastSeen(player.getUniqueId())
                .exceptionally(ex -> {
                    Bukkit.getLogger().warning("Erro ao atualizar o último login do jogador " + player.getName() + ": " + ex.getMessage());
                    return null;
                });
        scoreboardManager.removePlayer(player);
    }
}
