package dev.balafini.factions.command.arguments;

import dev.balafini.factions.command.FactionCommandArgument;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class DemoteCommand extends FactionCommandArgument {

    public DemoteCommand() {
        super("demote", "rebaixar");
    }

    @Override
    public boolean onArgument(@NotNull Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage("§cUso correto: /f rebaixar <jogador>");
            return false;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage("§cJogador não encontrado.");
            return false;
        }

        // TODO: implement faction chat notify
        membershipService.demoteMember(player.getUniqueId(), target.getUniqueId())
                .thenAccept(_ -> {
                    player.sendMessage("§aVocê rebaixou " + target.getName() + " na sua facção.");
                    if (target.isOnline()) {
                        target.sendMessage("§aVocê foi rebaixado na facção de " + player.getName() + ".");
                    }
                }).exceptionally(throwable -> {
                    player.sendMessage("§c" + throwable.getMessage());
                    return null;
                });
        return true;
    }
}
