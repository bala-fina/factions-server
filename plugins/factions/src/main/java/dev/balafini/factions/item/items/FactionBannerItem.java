package dev.balafini.factions.item.items;

import dev.balafini.factions.FactionsPlugin;
import dev.balafini.factions.faction.service.FactionQueryService;
import dev.balafini.factions.item.CustomItem;
import dev.balafini.factions.util.CooldownUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

public class FactionBannerItem extends CustomItem {

    private final FactionQueryService factionService;

    public FactionBannerItem(FactionsPlugin plugin, CooldownUtil cooldownUtil, FactionQueryService factionService) {
        super(plugin, cooldownUtil);
        this.factionService = factionService;
    }

    @Override
    public String getId() {
        return "faction_banner";
    }

    @Override
    public Duration getCooldown() {
        return Duration.ofMinutes(5);
    }

    @Override
    protected ItemStack createItem() {
        ItemStack item = new ItemStack(Material.RED_BANNER);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Banner da Facção", NamedTextColor.GOLD));
        meta.lore(List.of(
                Component.text("Concede +5% dano aos aliados no chunk", NamedTextColor.GRAY),
                Component.text("Duração: 5 minutos", NamedTextColor.YELLOW),
                Component.text("Cooldown: 5m", NamedTextColor.RED)
        ));

        item.setItemMeta(meta);
        return item;
    }

    @Override
    protected boolean executeAction(Player player, PlayerInteractEvent event) {
        factionService.findFactionByPlayer(player.getUniqueId())
                .thenAccept(faction -> {
                    if (faction.isEmpty()) {
                        player.sendMessage(Component.text(
                                "Você precisa estar em uma facção!",
                                NamedTextColor.RED
                        ));
                        return;
                    }

                    activateBanner(player, faction.get().factionId());
                });

        return true;
    }

    private void activateBanner(Player player, UUID factionId) {
        Location loc = player.getLocation();

        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc.clone().add(0, 2, 0), 20, 1, 1, 1, 0.1);
        player.getWorld().playSound(loc, Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);

        player.getWorld().getNearbyPlayers(loc, 20).forEach(nearby ->
                factionService.findFactionByPlayer(nearby.getUniqueId())
                        .thenAccept(nearbyFaction -> {
                            if (nearbyFaction.isPresent() && nearbyFaction.get().factionId().equals(factionId)) {
                                nearby.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 300, 1, false, false, true));
                                nearby.sendMessage(Component.text(
                                        player.getName() + " ativou o banner! Força concedida!",
                                        NamedTextColor.GREEN
                                ));
                            }
                        })
        );
    }


}
