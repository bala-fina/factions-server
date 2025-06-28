package dev.balafini.factions.faction.command.arguments;

import dev.balafini.factions.faction.command.FactionCommandArgument;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class HelpCommand extends FactionCommandArgument {

    public HelpCommand() {
        super("help", "ajuda", "h", "?");
    }

    @Override
    public boolean onArgument(@NotNull Player player, String[] args) {
        return sendHelpMessage(player);
    }

    private boolean sendHelpMessage(CommandSender sender) {
        sender.sendMessage(
                "",
                "§b§lCOMANDOS DO FACTIONS",
                "",
                "§f/f criar <tag> <nome> §8- §7Cria uma facção com o tag e nome especificados.",
                "§f/f desfazer §8- §7Desfaz a sua facção atual.",
                "§f/f info <tag> §8- §7Mostra informações sobre a facção com o tag especificado.",
                "§f/f entrar <tag> §8- §7Entra na facção com o tag especificado.",
                "§f/f sair §8- §7Sai da facção atual.",
                "§f/f convidar <jogador> §8- §7Convida um jogador para a sua facção.",
                "§f/f aceitar <tag> §8- §7Aceita um convite para a facção com o tag especificado.",
                "§f/f recusar <tag> §8- §7Recusa um convite para a facção com o tag especificado.",
                "§f/f listar §8- §7Lista todas as facções.",
                "§f/f ajuda §8- §7Mostra esta mensagem de ajuda.",
                "§f/f sair §8- §7Sai da facção atual.",
                "§f/f claim §8- §7Sai da facção atual.",
                ""
        );
        return true;
    }
}
