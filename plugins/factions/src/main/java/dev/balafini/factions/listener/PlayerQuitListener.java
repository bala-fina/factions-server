package dev.balafini.factions.listener;

import dev.balafini.factions.scoreboard.ScoreboardManager;
import dev.balafini.factions.service.user.UserService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

@SuppressWarnings("UnstableApiUsage")
public class PlayerQuitListener implements Listener {

    private final UserService userService;
    private final ScoreboardManager scoreboardManager;

    public PlayerQuitListener(UserService userService, ScoreboardManager scoreboardManager) {
        this.userService = userService;
        this.scoreboardManager = scoreboardManager;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        userService.updateUserLastSeen(player.getUniqueId())
                .exceptionally(ex -> {
                    Bukkit.getLogger().warning("Erro ao atualizar o último login do jogador " + player.getName() + ": " + ex.getMessage());
                    return null;
                });
        scoreboardManager.removePlayer(player);
    }
}
