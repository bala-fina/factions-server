package dev.balafini.factions.faction.command.arguments;

import dev.balafini.factions.faction.command.FactionCommandArgument;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.logging.Level;
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

        lifecycleService.createFaction(factionTag, factionName, player)
            .thenAccept(faction -> player.sendMessage("§aFacção criada com sucesso: [" + faction.tag() + "] " + faction.name()))
            .exceptionally(throwable -> {
                Bukkit.getLogger().log(Level.INFO, "Erro ao criar facção", throwable);
                player.sendMessage("§c" + throwable.getCause().getMessage());
                return null;
            });
        return true;
    }
}
