package dev.balafini.factions.item;

import dev.balafini.factions.FactionsPlugin;
import dev.balafini.factions.util.CooldownUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.time.Duration;
import java.util.Optional;

public abstract class CustomItem {

    protected final FactionsPlugin plugin;
    protected final CooldownUtil cooldownUtil;
    private final NamespacedKey itemIdKey;
    private ItemStack cachedItem;

    protected CustomItem(FactionsPlugin plugin, CooldownUtil cooldownUtil) {
        this.plugin = plugin;
        this.cooldownUtil = cooldownUtil;
        this.itemIdKey = new NamespacedKey(plugin, "custom_item_id");
    }

    public abstract String getId();
    protected abstract ItemStack createItem();
    public abstract Duration getCooldown();
    protected abstract boolean executeAction(Player player, PlayerInteractEvent event);

    public final ItemStack getItem() {
        if (cachedItem == null) {
            cachedItem = createItem();
            markAsCustomItem(cachedItem);
        }
        return cachedItem.clone();
    }

    public final void execute(Player player, PlayerInteractEvent event) {
        if (event.getItem() == null) return;

        if (isOnCooldown(player)) {
            sendCooldownMessage(player);
            return;
        }

        if (executeAction(player, event)) {
            consumeItem(event.getItem());
            applyCooldown(player);
        }
    }

    public final boolean matches(ItemStack item) {
        if (item == null || item.getItemMeta() == null) return false;

        String itemId = item.getItemMeta()
                .getPersistentDataContainer()
                .get(itemIdKey, PersistentDataType.STRING);

        return getId().equals(itemId);
    }

    private void markAsCustomItem(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(itemIdKey, PersistentDataType.STRING, getId());
            item.setItemMeta(meta);
        }
    }

    private boolean isOnCooldown(Player player) {
        return getCooldown().compareTo(Duration.ZERO) > 0 &&
               cooldownUtil.getTimeLeft(player.getUniqueId(), getId()).isPresent();
    }

    private void applyCooldown(Player player) {
        if (getCooldown().compareTo(Duration.ZERO) > 0) {
            cooldownUtil.setCooldown(player.getUniqueId(), getId(), getCooldown());
        }
    }

    private void consumeItem(ItemStack item) {
        item.subtract(1);
    }

    protected void sendCooldownMessage(Player player) {
        Optional<Duration> timeLeft = cooldownUtil.getTimeLeft(player.getUniqueId(), getId());
        timeLeft.ifPresent(duration -> {
            long seconds = duration.getSeconds();
            player.sendMessage(Component.text(
                    "Aguarde " + seconds + " segundos para usar novamente.",
                    NamedTextColor.RED
            ));
        });
    }
}
