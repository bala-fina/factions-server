package dev.balafini.factions.item.items;

import dev.balafini.factions.FactionsPlugin;
import dev.balafini.factions.item.CustomItem;
import dev.balafini.factions.util.CooldownUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.Duration;
import java.util.List;
import java.util.Set;

public class LightningMasterItem extends CustomItem {

    public LightningMasterItem(FactionsPlugin plugin, CooldownUtil cooldownUtil) {
        super(plugin, cooldownUtil);
    }

    @Override
    public String getId() {
        return "lightning_master";
    }

    @Override
    public Duration getCooldown() {
        return Duration.ofSeconds(5);
    }

    @Override
    protected ItemStack createItem() {
        ItemStack item = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Raio Mestre", NamedTextColor.YELLOW));
        meta.lore(List.of(
                Component.text("Invoca um raio onde você olha", NamedTextColor.GRAY),
                Component.text("Cooldown: 15s", NamedTextColor.RED)
        ));

        item.setItemMeta(meta);
        return item;
    }

    @Override
    protected boolean executeAction(Player player, PlayerInteractEvent event) {
        Block target = player.getTargetBlock(Set.of(Material.AIR), 4);
        Location strike = target.getLocation();

        if (target.getType() == Material.AIR) {
            strike = player.getEyeLocation()
                    .add(player.getEyeLocation().getDirection().multiply(15));
        }

        player.getWorld().strikeLightning(strike.add(0.5, 0, 0.5));
        return true;
    }
}
