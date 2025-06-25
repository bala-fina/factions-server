package dev.balafini.factions.command.arguments;

import dev.balafini.factions.command.FactionCommandArgument;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.stream.Collectors;

public class CreateCommand extends FactionCommandArgument {

    public CreateCommand() {
        super("create", "criar");
    }

    @Override
    public boolean onArgument(@NotNull Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cVocê precisa fornecer um nome e tag para a facção.");
            return false;
        }

        String factionTag = args[0].toUpperCase();
        String factionName = Arrays.stream(args, 1, args.length)
            .collect(Collectors.joining(" "));

        lifecycleService.createFaction(factionTag, factionName, player.getUniqueId(), player.getName())
            .thenAccept(faction -> player.sendMessage("§aFacção criada com sucesso: [" + faction.tag() + "] " + faction.name()))
            .exceptionally(throwable -> {
                player.sendMessage("§c" + throwable.getMessage());
                return null;
            });
        return true;
    }
}
