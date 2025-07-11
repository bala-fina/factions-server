package dev.balafini.factions.faction.command.arguments;

import dev.balafini.factions.faction.command.FactionCommandArgument;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class KickCommand extends FactionCommandArgument {

    public KickCommand() {
        super("kick", "expulsar");
    }

    @Override
    public boolean onArgument(@NotNull Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage("§cUso correto: /f expulsar <jogador>");
            return false;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage("§cJogador não encontrado.");
            return false;
        }

        // TODO: implement faction chat notify
        membershipService.kickMember(player.getUniqueId(), target.getUniqueId())
                .thenAccept(_ -> {
                    player.sendMessage("§aVocê expulsou " + target.getName() + " da sua facção.");
                    if (target.isOnline()) {
                        target.sendMessage("§cVocê foi expulso da facção de " + player.getName() + ".");
                    }
                }).exceptionally(throwable -> {
                    player.sendMessage("§c" + throwable.getCause().getMessage());
                    return null;
                });


        return true;
    }
}
