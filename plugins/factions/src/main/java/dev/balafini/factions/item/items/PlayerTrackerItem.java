package dev.balafini.factions.item.items;

import dev.balafini.factions.FactionsPlugin;
import dev.balafini.factions.item.CustomItem;
import dev.balafini.factions.util.CooldownUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.Duration;
import java.util.List;

public class PlayerTrackerItem extends CustomItem {

    public PlayerTrackerItem(FactionsPlugin plugin, CooldownUtil cooldownUtil) {
        super(plugin, cooldownUtil);
    }

    @Override
    public String getId() {
        return "player_tracker";
    }

    @Override
    public Duration getCooldown() {
        return Duration.ofSeconds(45);
    }

    @Override
    protected ItemStack createItem() {
        ItemStack item = new ItemStack(Material.RECOVERY_COMPASS);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Rastreador de Jogadores", NamedTextColor.GOLD));
        meta.lore(List.of(
                Component.text("Detecta jogadores em 50 blocos", NamedTextColor.GRAY),
                Component.text("Cooldown: 45s", NamedTextColor.RED)
        ));

        item.setItemMeta(meta);
        return item;
    }

    @Override
    protected boolean executeAction(Player player, PlayerInteractEvent event) {
        List<Player> nearby = player.getWorld()
                .getNearbyPlayers(player.getLocation(), 50, p -> !p.equals(player))
                .stream().toList();

        if (nearby.isEmpty()) {
            player.sendMessage(Component.text("Nenhum jogador encontrado.", NamedTextColor.RED));
            return true;
        }

        player.sendMessage(Component.text(
                "Encontrados " + nearby.size() + " jogadores!",
                NamedTextColor.GREEN
        ));

        nearby.forEach(p -> {
            p.setGlowing(true);
            Bukkit.getScheduler().runTaskLater(plugin, () -> p.setGlowing(false), 200L);
        });

        return true;
    }
}

