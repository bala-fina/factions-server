package dev.balafini.factions.listener;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

public class PlayerTeleportListener implements Listener {

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if ( event.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
            return;
        }

        Location to = event.getTo();

        if (!isSafeTeleportLocation(to)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.text("Você não pode teleportar para este local!"));
        }
    }

    private boolean isSafeTeleportLocation(Location location) {
        Block feetBlock = location.getBlock();
        Block headBlock = location.clone().add(0, 1, 0).getBlock();
        Block groundBlock = location.clone().subtract(0, 1, 0).getBlock();

        return groundBlock.getType().isSolid() && !feetBlock.getType().isSolid() && !headBlock.getType().isSolid()
               && !Tag.PORTALS.isTagged(feetBlock.getType()) && !Tag.PORTALS.isTagged(headBlock.getType());
    }
}
