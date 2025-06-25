package dev.balafini.factions.command.arguments;

import dev.balafini.factions.command.FactionCommandArgument;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CreateCommand extends FactionCommandArgument {

    public CreateCommand() {
        super("create", "criar");
    }

    @Override
    public boolean onArgument(@NotNull Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage("§cVocê precisa fornecer um nome para a facção.");
            return false;
        }

        String factionTag = args[0];
        if (factionTag.length() != 3) {
            player.sendMessage("§cO nome da facção deve ter 3 caracteres.");
            return false;
        }

        if (!factionTag.matches("[a-zA-Z0-9]+")) {
            player.sendMessage("§cO nome da facção só pode conter letras e números.");
            return false;
        }

        String factionName = String.join(" ", args);
        if (factionName.length() < 5 || factionName.length() > 16) {
            player.sendMessage("§cO nome da facção deve ter entre 5 e 16 caracteres.");
            return false;
        }

        if (!factionName.matches("[a-zA-Z0-9 ]+")) {
            player.sendMessage("§cO nome da facção só pode conter letras, números e espaços.");
            return false;
        }

        lifecycleService.createFaction(factionTag, factionName, player.getUniqueId(), player.getName())
            .thenAccept(faction -> player.sendMessage("§aFacção criada com sucesso: [" + faction.tag() + "] " + faction.name()));
        return true;
    }
}
