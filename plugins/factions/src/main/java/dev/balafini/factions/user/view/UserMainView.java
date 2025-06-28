package dev.balafini.factions.user.view;

import com.google.common.collect.ImmutableMap;
import dev.balafini.factions.FactionsPlugin;
import dev.balafini.factions.faction.service.FactionInviteService;
import dev.balafini.factions.user.User;
import dev.balafini.factions.util.FormatterUtil;
import dev.balafini.factions.util.ItemBuilder;
import dev.balafini.factions.util.ViewUtil;
import me.saiintbrisson.minecraft.View;
import me.saiintbrisson.minecraft.ViewContext;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class UserMainView extends View {

    private final FactionsPlugin plugin;
    private final FactionInviteService factionInviteService;

    public UserMainView(FactionsPlugin plugin, FactionInviteService factionInviteService) {
        super(27, "§8Menu de Facção");
        this.plugin = plugin;
        this.factionInviteService = factionInviteService;
        ViewUtil.cancelAllDefaultActions(this);
    }

    @Override
    protected void onRender(@NotNull ViewContext context) {
        Player player = context.getPlayer();
        User user = context.get("user");

        if (user == null) {
            player.sendMessage("§cErro: Não foi possível carregar suas informações.");
            context.close();
            return;
        }

        // --- REFACTORED: Item creation using ItemBuilder ---
        context.slot(11).withItem(new ItemBuilder(Material.PLAYER_HEAD)
                .name("§a" + player.getName())
                .lore(
                        " ",
                        "§7Fação: §cNenhuma",
                        "§7Poder: §f" + (int) user.power() + "/" + (int) user.maxPower(),
                        "§7Abates: §a" + user.kills(),
                        "§7Mortes: §c" + user.deaths(),
                        "§7KDR: §f" + FormatterUtil.formatKdr(user.getKdr()),
                        " "
                )
                .skullOwner(player)
                .build());

        context.slot(13).withItem(new ItemBuilder(Material.WRITABLE_BOOK)
                        .name("§eConvites Pendentes")
                        .lore(
                                "§7Veja e gerencie os convites",
                                "§7pendentes para facções.",
                                "",
                                "§aClique para acessar."
                        )
                        .build())
                .onClick(handler -> {
                    factionInviteService.findInvitesByInviteeId(player.getUniqueId())
                            .whenComplete((invites, error) -> {
                                if (error != null) {
                                    player.sendMessage("§cOcorreu um erro ao buscar seus convites.");
                                    throw new RuntimeException("Error fetching invites", error);
                                }
                                Bukkit.getScheduler().runTask(plugin, () ->
                                        handler.open(UserInvitePaginatedView.class, ImmutableMap.of("invites", invites))
                                );
                            });
                });

        context.slot(15).withItem(new ItemBuilder(Material.FEATHER)
                .name("§ePreferências")
                .lore("§7Gerencie suas preferências.")
                .build());
    }
}
