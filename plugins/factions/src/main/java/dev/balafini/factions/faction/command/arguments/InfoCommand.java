package dev.balafini.factions.faction.command.arguments;

import dev.balafini.factions.faction.command.FactionCommandArgument;
import dev.balafini.factions.faction.Faction;
import dev.balafini.factions.util.FormatterUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class InfoCommand extends FactionCommandArgument {

    public InfoCommand() {
        super("info", "ver");
    }

    @Override
    public boolean onArgument(@NotNull Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage("§cUso correto: /f info <tag>");
            return false;
        }
        String tag = args[0].toUpperCase();

        queryService.findFactionByTag(tag)
                .thenCompose(optFaction -> {
                    if (optFaction.isPresent()) {
                        return CompletableFuture.completedFuture(optFaction);
                    }
                    return queryService.findFactionByTag(tag);
                })
                .thenAccept(optFaction -> {
                    if (optFaction.isEmpty()) {
                        player.sendMessage("§cFacção não encontrada com a tag: " + tag);
                        return;
                    }

                    Faction faction = optFaction.get();
                    sendFactionInfo(player, faction);
                });


        return true;
    }

    private void sendFactionInfo(Player player, Faction faction) {
        player.sendMessage("§7§m----------------------------------");
        player.sendMessage(" §eInformações da Facção: §6" + faction.name() + " [" + faction.tag() + "]");
        player.sendMessage(" ");
        player.sendMessage(" §fLíder: §7" + Bukkit.getOfflinePlayer(faction.getLeader().playerId()).getName());
        player.sendMessage(" §fPoder: §c" + (int)faction.power() + "§7/§c" + (int)faction.maxPower());
        player.sendMessage(" §fMembros: §a" + faction.getMembers().size() + "/" + 20);
        player.sendMessage(" §fData de criação: §f" + FormatterUtil.formatDate(faction.createdAt()));
        player.sendMessage("§7§m----------------------------------");
    }

}
