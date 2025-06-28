package dev.balafini.factions.faction.command.arguments;

import dev.balafini.factions.faction.command.FactionCommandArgument;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class DenyCommand extends FactionCommandArgument {

    public DenyCommand() {
        super("deny", "recusar");
    }

    @Override
    public boolean onArgument(@NotNull Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage("§cUso correto: /f recusar <tag>");
            return false;
        }
        String tag = args[0].toUpperCase();

        inviteService.denyInvite(player.getUniqueId(), tag)
                .thenAccept(_ -> player.sendMessage("§aVocê recusou o convite."))
                .exceptionally(throwable -> {
                    player.sendMessage("§c" + throwable.getCause().getMessage());
                    return null;
                });
        return true;
    }
}
