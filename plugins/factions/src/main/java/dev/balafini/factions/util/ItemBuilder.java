package dev.balafini.factions.util;

import com.google.common.base.Preconditions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ItemBuilder {
    private final ItemStack itemStack;
    private final ItemMeta itemMeta;

    public ItemBuilder(@NotNull Material material) {
        Preconditions.checkNotNull(material, "Material cannot be null");
        this.itemStack = new ItemStack(material);
        this.itemMeta = itemStack.getItemMeta();
    }

    public ItemBuilder(@NotNull ItemStack itemStack) {
        Preconditions.checkNotNull(itemStack, "ItemStack cannot be null");
        this.itemStack = itemStack.clone(); // Clone to avoid modifying the original
        this.itemMeta = this.itemStack.getItemMeta();
    }

    public ItemBuilder name(@NotNull String name) {
        Component component = LegacyComponentSerializer.legacyAmpersand().deserialize(name);
        itemMeta.displayName(component.decoration(TextDecoration.ITALIC, false));
        return this;
    }

    public ItemBuilder lore(@NotNull List<String> lore) {
        List<Component> components = lore.stream()
                .map(line -> LegacyComponentSerializer.legacyAmpersand().deserialize(line))
                .map(component -> component.decoration(TextDecoration.ITALIC, false))
                .collect(Collectors.toList());
        itemMeta.lore(components);
        return this;
    }

    public ItemBuilder lore(@NotNull String... lore) {
        return lore(Arrays.asList(lore));
    }

    public ItemBuilder enchant(@NotNull Enchantment enchantment, int level) {
        itemMeta.addEnchant(enchantment, level, true);
        return this;
    }

    public ItemBuilder flag(@NotNull ItemFlag... flags) {
        itemMeta.addItemFlags(flags);
        return this;
    }

    public ItemBuilder amount(int amount) {
        itemStack.setAmount(amount);
        return this;
    }

    public ItemBuilder skullOwner(@NotNull Player player) {
        if (itemMeta instanceof SkullMeta skullMeta) {
            skullMeta.setPlayerProfile(player.getPlayerProfile());
        }
        return this;
    }

    public ItemStack build() {
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }
}
