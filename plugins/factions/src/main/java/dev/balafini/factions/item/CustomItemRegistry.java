package dev.balafini.factions.item;

import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CustomItemRegistry {

    private final Map<String, CustomItem> items = new HashMap<>();

    public void register(CustomItem item) {
        items.put(item.getId(), item);
    }

    public Optional<CustomItem> findItem(ItemStack itemStack) {
        return items.values().stream()
                .filter(item -> item.matches(itemStack))
                .findFirst();
    }

    public Optional<ItemStack> getItem(String id) {
        return Optional.ofNullable(items.get(id))
                .map(CustomItem::getItem);
    }

    public Map<String, CustomItem> getAll() {
        return Map.copyOf(items);
    }
}
