package dev.balafini.factions.command.arguments;

import dev.balafini.factions.command.FactionCommandArgument;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class LeaveCommand extends FactionCommandArgument {

    public LeaveCommand() {
        super("leave", "sair");
    }

    @Override
    public boolean onArgument(@NotNull Player player, String[] args) {
        // TODO: implement faction chat notify
        membershipService.leaveFaction(player.getUniqueId()).thenAccept(_ -> player.sendMessage("§aVocê saiu da facção com sucesso."))
                .exceptionally(throwable -> {
                    player.sendMessage("§c" + throwable.getCause().getMessage());
                    return null;
                }
        );
        return true;
    }
}
