package dev.balafini.factions.listener;

import dev.balafini.factions.user.service.UserCombatService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

@SuppressWarnings("UnstableApiUsage")
public class PlayerDeathListener implements Listener {

    private final UserCombatService userCombatService;

    public PlayerDeathListener(UserCombatService userCombatService) {
        this.userCombatService = userCombatService;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer != null && !killer.equals(victim)) {
            userCombatService.handlePlayerKill(killer.getUniqueId(), killer.getName(), victim.getUniqueId(), victim.getName())
                    .exceptionally(ex -> {
                        Bukkit.getLogger().warning("Falha ao processar o abate de " + killer.getName() + " em " + victim.getName() + ": " + ex.getMessage());
                        return null;
                    });
        } else {
            userCombatService.handlePveOrSuicideDeath(victim.getUniqueId(), victim.getName())
                    .exceptionally(ex -> {
                        Bukkit.getLogger().warning("Falha ao processar a morte de " + victim.getName() + ": " + ex.getMessage());
                        return null;
                    });
        }
    }
}

