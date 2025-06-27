package dev.balafini.factions.item.items;

import dev.balafini.factions.FactionsPlugin;
import dev.balafini.factions.item.CustomItem;
import dev.balafini.factions.util.CooldownUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;

import java.time.Duration;
import java.util.List;

public class EnderPearlItem extends CustomItem {

    public static final String PEARL_METADATA = "custom_ender_pearl";

    public EnderPearlItem(FactionsPlugin plugin, CooldownUtil cooldownUtil) {
        super(plugin, cooldownUtil);
    }

    @Override
    public String getId() {
        return "ender_pearl";
    }

    @Override
    public Duration getCooldown() {
        return Duration.ofSeconds(15);
    }

    @Override
    protected ItemStack createItem() {
        ItemStack item = new ItemStack(Material.ENDER_PEARL);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Pérola do Fim", NamedTextColor.DARK_PURPLE));
        meta.lore(List.of(
                Component.text("Lança uma pérola do fim", NamedTextColor.GRAY),
                Component.text("Cooldown: 15s", NamedTextColor.RED)
        ));

        item.setItemMeta(meta);
        return item;
    }

    @Override
    protected boolean executeAction(Player player, PlayerInteractEvent event) {
        event.setCancelled(true);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);

        EnderPearl pearl = player.launchProjectile(EnderPearl.class);
        pearl.setVelocity(player.getEyeLocation().getDirection().multiply(1.5));
        pearl.setShooter(player);
        pearl.setMetadata(PEARL_METADATA, new FixedMetadataValue(plugin, true));

        return true;
    }
}
