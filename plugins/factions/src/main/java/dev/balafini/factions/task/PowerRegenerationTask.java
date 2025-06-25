package dev.balafini.factions.task;

import dev.balafini.factions.user.service.UserPowerService;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

@SuppressWarnings("UnstableApiUsage")
public class PowerRegenerationTask extends BukkitRunnable {

    private final UserPowerService userPowerService;

    public PowerRegenerationTask(UserPowerService userPowerService) {
        this.userPowerService = userPowerService;
    }

    @Override
    public void run() {
        Bukkit.getOnlinePlayers().forEach(player ->
                userPowerService.regeneratePower(player.getUniqueId(), player.getName())
                        .exceptionally(ex -> {
                            Bukkit.getLogger().warning("Failed to regenerate power for " + player.getName() + ": " + ex.getMessage());
                            return null;
                        })
        );
    }
}
