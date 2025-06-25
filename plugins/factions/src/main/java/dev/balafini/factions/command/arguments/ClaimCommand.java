package dev.balafini.factions.command.arguments;

import dev.balafini.factions.command.FactionCommandArgument;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ClaimCommand extends FactionCommandArgument {

    public ClaimCommand() {
        super("claim", "claimar", "dominar");
    }

    @Override
    public boolean onArgument(@NotNull Player player, String[] args) {
        return false;
    }
}
