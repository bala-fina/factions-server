package dev.balafini.factions.util;

import dev.balafini.factions.faction.Faction;
import me.saiintbrisson.minecraft.AbstractView;
import me.saiintbrisson.minecraft.VirtualView;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.jetbrains.annotations.NotNull;


public class ViewUtil {

    public static ItemStack QUIT_ITEM = new ItemBuilder(Material.BARRIER)
            .name("§cSair")
            .lore("§7Clique para sair do menu")
            .flag(ItemFlag.HIDE_ATTRIBUTES)
            .build();

    public static ItemStack BACK_ITEM = new ItemBuilder(Material.ARROW)
            .name("§cVoltar")
            .lore("§7Clique para voltar ao menu anterior")
            .flag(ItemFlag.HIDE_ATTRIBUTES)
            .build();

    public static ItemStack NEXT_ITEM = new ItemBuilder(Material.ARROW)
            .name("§aPróximo")
            .lore("§7Clique para ir para a próxima página")
            .flag(ItemFlag.HIDE_ATTRIBUTES)
            .build();

    public static ItemStack CONFIRM_ITEM = new ItemBuilder(Material.LIME_WOOL)
            .name("§aConfirmar")
            .lore("§7Clique para confirmar a ação")
            .flag(ItemFlag.HIDE_ATTRIBUTES)
            .build();

    public static ItemStack CANCEL_ITEM = new ItemBuilder(Material.RED_WOOL)
            .name("§cCancelar")
            .lore("§7Clique para cancelar a ação")
            .flag(ItemFlag.HIDE_ATTRIBUTES)
            .build();

    public static ItemStack FILLER_ITEM = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
            .name(" ")
            .build();

    public static void cancelAllDefaultActions(@NotNull AbstractView view) {
        view.setCancelOnClick(true);
        view.setCancelOnClone(true);
        view.setCancelOnDrag(true);
        view.setCancelOnDrop(true);
        view.setCancelOnMoveOut(true);
        view.setCancelOnPickup(true);
        view.setCancelOnShiftClick(true);
    }

    public static void fillBorder(@NotNull VirtualView view, @NotNull ItemStack edge) {
        int sizeInventory = view.getSize();

        for (int i = 0; i < 9; i++) {
            view.slot(i).withItem(edge);
        }

        for (int i = sizeInventory - 9; i < sizeInventory; i++) {
            view.slot(i).withItem(edge);
        }

        for (int i = 0; i < (sizeInventory / 9) - 1; i++) {
            view.slot(i * 9).withItem(edge);
            view.slot(i * 9 + 8).withItem(edge);
        }
    }

    public static ItemStack createFactionBanner(Faction faction) {
        ItemStack banner = new ItemBuilder(Material.WHITE_BANNER)
                .flag(ItemFlag.HIDE_ATTRIBUTES)
                .build();

        BannerMeta meta = (BannerMeta) banner.getItemMeta();
        if (meta != null) {
            meta.addPattern(new Pattern(DyeColor.BLACK, PatternType.STRIPE_MIDDLE));
            banner.setItemMeta(meta);
        }
        return banner;
    }

}
