package dev.balafini.factions.user.view;

import dev.balafini.factions.faction.FactionInvite;
import dev.balafini.factions.util.FormatterUtil;
import dev.balafini.factions.util.ItemBuilder;
import dev.balafini.factions.util.ViewUtil;
import me.saiintbrisson.minecraft.PaginatedView;
import me.saiintbrisson.minecraft.PaginatedViewSlotContext;
import me.saiintbrisson.minecraft.ViewContext;
import me.saiintbrisson.minecraft.ViewItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class UserInvitePaginatedView extends PaginatedView<FactionInvite> {

    private static final ItemStack NO_INVITES_ITEM = new ItemBuilder(Material.COBWEB)
            .name("§cVazio")
            .lore("§7Você não possui convites pendentes.")
            .build();

    public UserInvitePaginatedView() {
        super(5, "Convites Pendentes");

        setNextPageItem((context, item) -> item.setItem(context.isLastPage() ? null : ViewUtil.NEXT_ITEM));
        setPreviousPageItem((context, item) -> item.setItem(context.isFirstPage() ? null : ViewUtil.BACK_ITEM));

        setLayout(
                "YYYYYYYYY",
                "YOOOOOOOY",
                "YOOOOOOOY",
                "YYYYYYYYY",
                "Y<YYYYY>Y"
        );

        setLayout(
                "YYYYYYYYY",
                "YOOOOOOOY",
                "YOOOOOOOY",
                "YYYYYYYYY",
                "Y<YYYYY>Y"
        );

        setLayout('Y', viewItem -> viewItem.setItem(ViewUtil.FILLER_ITEM));

        ViewUtil.cancelAllDefaultActions(this);
    }

    @Override
    protected void onRender(@NotNull ViewContext context) {

        List<FactionInvite> invites = context.get("invites");

        if (invites == null || invites.isEmpty()) {
            context.paginated().setSource(Collections.emptyList());
            context.slot(22, NO_INVITES_ITEM);
            return;
        }

        context.paginated().setSource(invites);
    }

    @Override
    protected void onItemRender(@NotNull PaginatedViewSlotContext<FactionInvite> context, @NotNull ViewItem viewItem, @NotNull FactionInvite invite) {
        UUID inviterId = invite.inviterId();
        OfflinePlayer inviter = Bukkit.getOfflinePlayer(inviterId);
        String inviterName = inviter.getName() != null ? inviter.getName() : "Desconhecido";

        ItemStack banner = ViewUtil.createFactionBanner(null);

        viewItem.withItem(new ItemBuilder(banner)
                        .name("§aConvite de: §f" + invite.factionTag())
                        .lore(
                                " ",
                                "§7Convidado por: §e" + inviterName,
                                "§7Data: §f" + FormatterUtil.formatDate(invite.invitedAt()),
                                " ",
                                "§aClique para aceitar o convite.",
                                "§cClique-Direito para recusar."
                        )
                        .build())
                .onClick(handler -> {
                    if (handler.isLeftClick()) {
                        handler.getPlayer().performCommand("f aceitar " + invite.factionTag());
                        handler.close();
                    }
                    else if (handler.isRightClick()) {
                        handler.getPlayer().performCommand("f recusar " + invite.factionTag());
                        handler.open(UserInvitePaginatedView.class, context.getData());
                    }
                });
    }
}