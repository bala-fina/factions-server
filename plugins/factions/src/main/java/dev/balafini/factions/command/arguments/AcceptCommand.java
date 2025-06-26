package dev.balafini.factions.command.arguments;

import dev.balafini.factions.command.FactionCommandArgument;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class AcceptCommand extends FactionCommandArgument {

    public AcceptCommand() {
        super("join", "aceitar", "accept", "entrar");
    }

    @Override
    public boolean onArgument(@NotNull Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage("§cUso correto: /f aceitar <tag>");
            return false;
        }

        String tag = args[0].toUpperCase();

        // TODO: implement faction chat notify
        inviteService.acceptInvite(player.getUniqueId(), player.getName(), tag)
                .thenAccept(faction -> player.sendMessage("§aVocê entrou na facção §f" + faction.tag()))
                .exceptionally(throwable -> {
                    player.sendMessage("§c" + throwable.getCause().getMessage());
                    return null;
                });
        return true;
    }
}
