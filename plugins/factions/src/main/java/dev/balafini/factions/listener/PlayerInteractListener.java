package dev.balafini.factions.listener;

import dev.balafini.factions.item.CustomItemRegistry;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

public class PlayerInteractListener implements Listener {

    private final CustomItemRegistry registry;

    public PlayerInteractListener(CustomItemRegistry registry) {
        this.registry = registry;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.getAction().isRightClick() || event.getItem() == null) return;

        registry.findItem(event.getItem()).ifPresent(item -> {
            event.setCancelled(true);
            item.execute(event.getPlayer(), event);
        });
    }

}
