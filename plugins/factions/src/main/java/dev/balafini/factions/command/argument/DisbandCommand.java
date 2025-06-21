package dev.balafini.factions.command.argument;

import dev.balafini.factions.command.FactionCommandArgument;
import dev.balafini.factions.service.faction.FactionService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("UnstableApiUsage")
public class DisbandCommand extends FactionCommandArgument {

    private final FactionService factionService;

    public DisbandCommand(FactionService factionService) {
        super("disband", "desfazer");

        this.factionService = factionService;
    }

    @Override
    public boolean onArgument(@NotNull Player player, String[] args) {
        factionService.findFactionByPlayer(player.getUniqueId()).thenCompose(optFaction -> {
            if (optFaction.isEmpty()) {
                player.sendMessage("§cVocê não faz parte de nenhuma facção.");
                return CompletableFuture.completedFuture(null);
            }
            return factionService.disbandFaction(optFaction.get().factionId(), player.getUniqueId())
                    .thenAccept(v -> player.sendMessage("§aSua facção foi desfeita com sucesso!"));
        }).exceptionally(ex -> {
            player.sendMessage("§cOcorreu um erro ao desfazer sua facção.");
            Bukkit.getLogger().warning(ex.getMessage());
            return null;
        });

        return true;
    }
}
