package dev.balafini.factions.command.arguments;

import dev.balafini.factions.command.FactionCommandArgument;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class InviteCommand extends FactionCommandArgument {

    public InviteCommand() {
        super("invite", "convidar");
    }

    @Override
    public boolean onArgument(@NotNull Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage("§cUso correto: /f expulsar <jogador>");
            return false;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage("§cO jogador especificado não está online.");
            return false;
        }
        inviteService.invitePlayer(player.getUniqueId(), target.getUniqueId())
            .thenAccept(_ -> {
                player.sendMessage("§aVocê convidou " + target.getName() + " para a sua facção.");
                target.sendMessage("§aVocê foi convidado para a facção de " + player.getName() + ".");
            })
            .exceptionally(throwable -> {
                player.sendMessage("§c" + throwable.getCause().getMessage());
                return null;
            });
        return true;
    }
}
