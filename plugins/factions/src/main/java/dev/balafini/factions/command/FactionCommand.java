package dev.balafini.factions.command;

import dev.balafini.factions.command.arguments.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FactionCommand extends Command {

    private final Set<FactionCommandArgument> argumentSet = new HashSet<>();

    public FactionCommand() {
        super("factions");
        setAliases(List.of("factions", "faccoes", "f"));

        argumentSet.addAll(Set.of(
                new CreateCommand(),
                new DisbandCommand(),
                new LeaveCommand(),
                new InviteCommand(),
                new AcceptCommand(),
                new DenyCommand(),
                new DemoteCommand(),
                new PromoteCommand(),
                new KickCommand(),
                new InfoCommand()
                ));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cVocê precisa ser um jogador para executar esse comando.");
            return false;
        }

        if (args.length == 0 || args[0].equals("help") || args[0].equals("?") || args[0].equals("ajuda")) {
            return sendHelpMessage(sender);
        }

        FactionCommandArgument factionArgument = argumentSet.stream()
                .filter(argument -> argument.matchArgument(args[0]))
                .findFirst()
                .orElse(null);

        if (factionArgument == null) {
            return sendHelpMessage(sender);
        }

        return factionArgument.onArgument(player, Arrays.copyOfRange(args, 1, args.length));
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

    /*
       Implemented commands:
            create, disband, leave, invite, accept, deny, demote, promote, kick, info
       Remaining commands to implement:
             list, claim
     */

}
