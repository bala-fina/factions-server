package dev.balafini.factions.item.command;

import dev.balafini.factions.item.CustomItemRegistry;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Optional;

public class GiveCustomItemCommand extends Command {

    private final CustomItemRegistry itemRegistry;

    public GiveCustomItemCommand(CustomItemRegistry itemRegistry) {
        super("givecustomitem");
        this.itemRegistry = itemRegistry;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cVocê precisa ser um jogador para executar esse comando.");
            return false;
        }

        if (!player.hasPermission("factions.give.customitem")) {
            player.sendMessage("§cVocê não tem permissão para usar esse comando.");
            return false;
        }

        if (args.length < 1) {
            player.sendMessage("§cUso correto: /givecustomitem <item_id> [quantidade]");
            return false;
        }

        String itemId = args[0].toLowerCase();
        Optional<ItemStack> optItem = itemRegistry.getItem(itemId);

        if (optItem.isEmpty()) {
            sender.sendMessage("§cCustom item '" + itemId + "' não encontrado. Disponíveis: " + String.join(", ", itemRegistry.getAll().keySet()));
            return false;
        }

        int amount = 1;
        if (args.length >= 2) {
            try {
                amount = Integer.parseInt(args[1]);
                if (amount <= 0) {
                    player.sendMessage("§cQuantidade deve ser maior que zero.");
                    return false;
                }
                if (amount > 64) {
                    player.sendMessage("§cQuantidade máxima é 64.");
                    return false;
                }
            } catch (NumberFormatException e) {
                player.sendMessage("§cQuantidade inválida. Use um número inteiro.");
                return false;
            }
        }

        if (!hasInventorySpace(player, amount)) {
            player.sendMessage("§cInventário cheio! Libere espaço para receber os itens.");
            return false;
        }

        ItemStack customItem = optItem.get().clone();
        customItem.setAmount(amount);

        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(customItem);

        if (leftover.isEmpty()) {
            player.sendMessage("§aVocê recebeu " + amount + "x " + itemId + ".");
        }

        return true;
    }

    private boolean hasInventorySpace(Player player, int amount) {
        Inventory inv = player.getInventory();
        int freeSlots = 0;

        for (ItemStack item : inv.getStorageContents()) {
            if (item == null || item.getType() == Material.AIR) {
                freeSlots++;
            }
        }

        int slotsNeeded = (int) Math.ceil((double) amount / 64);

        return freeSlots >= slotsNeeded;
    }
}
