package dev.balafini.factions.faction.command.arguments;

import dev.balafini.factions.faction.command.FactionCommandArgument;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class DisbandCommand extends FactionCommandArgument {

    private final List<UUID> confirmState = new ArrayList<>();

    public DisbandCommand() {
        super("disband", "desfazer");
    }

    @Override
    public boolean onArgument(@NotNull Player player, String[] args) {
        if (confirmState.stream().noneMatch(uuid -> uuid.equals(player.getUniqueId()))) {
            player.sendMessage("§cVocê tem certeza que deseja desfazer sua facção? Digite /f desfazer para confirmar.");
            confirmState.add(player.getUniqueId());
            return false;
        }

        lifecycleService.disbandFaction(player.getUniqueId())
                .thenRun(() -> {
                    player.sendMessage("§aSua facção foi desfeita com sucesso.");
                    confirmState.remove(player.getUniqueId());
                })
                .exceptionally(throwable -> {
                    player.sendMessage("§c" + throwable.getLocalizedMessage());
                    confirmState.remove(player.getUniqueId());
                    return null;
                });
        return true;
    }
}
