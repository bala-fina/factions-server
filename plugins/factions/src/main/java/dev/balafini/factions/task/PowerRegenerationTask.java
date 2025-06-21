package dev.balafini.factions.task;

import dev.balafini.factions.service.user.UserService;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

public class PowerRegenerationTask extends BukkitRunnable {

    private final UserService userService;

    public PowerRegenerationTask(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void run() {
        Bukkit.getOnlinePlayers().forEach(player -> userService.regeneratePower(player.getUniqueId())
                .exceptionally(ex -> {
                    throw new RuntimeException("Falha ao regenerar poder para " + player.getName(), ex);
                }));
    }
}
