package dev.balafini.factions.faction.command.arguments;

import dev.balafini.factions.faction.command.FactionCommandArgument;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

public class ClaimCommand extends FactionCommandArgument {

    public ClaimCommand() {
        super("claim", "claimar", "dominar");
    }

    @Override
    public boolean onArgument(@NotNull Player player, String[] args) {
        claimService.claimChunk(player, player.getLocation().getChunk())
                .thenAccept(claim -> player.sendMessage("§aVocê dominou o chunk " + claim.getChunk().getX() + ", " + claim.getChunk().getZ() + " com sucesso."))
                .exceptionally(throwable -> {
                    Bukkit.getLogger().log(Level.INFO, "Erro ao claimar chunk", throwable);
                    player.sendMessage("§c" + throwable.getCause().getMessage());
                    return null;
                });

        return true;
    }
}
