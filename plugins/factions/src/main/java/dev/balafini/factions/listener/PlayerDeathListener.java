package dev.balafini.factions.listener;

import dev.balafini.factions.service.user.UserService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

@SuppressWarnings("UnstableApiUsage")
public class PlayerDeathListener implements Listener {

    private final UserService userService;

    public PlayerDeathListener(UserService userService) {
        this.userService = userService;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();

        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer != null && !killer.equals(victim)) {
            userService.handlePlayerKill(killer.getUniqueId(), victim.getUniqueId())
                    .exceptionally(ex -> {
                        Bukkit.getLogger().warning("Falha ao processar o abate de " + killer.getName() + " em " + victim.getName() + ": " + ex.getMessage());
                        return null;
                    });
        } else {
            userService.handlePveOrSuicideDeath(victim.getUniqueId())
                    .exceptionally(ex -> {
                        Bukkit.getLogger().warning("Falha ao processar a morte de " + victim.getName() + ": " + ex.getMessage());
                        return null;
                    });
        }
    }
}

