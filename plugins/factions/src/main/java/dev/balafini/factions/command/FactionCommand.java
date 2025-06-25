package dev.balafini.factions.command;

import dev.balafini.factions.FactionsPlugin;
import dev.balafini.factions.faction.service.*;
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

    protected FactionClaimService claimService;
    protected FactionInviteService inviteService;
    protected FactionLifecycleService lifecycleService;
    protected FactionMembershipService membershipService;
    protected FactionQueryService queryService;
    protected FactionStatsService statsService;

    public FactionCommand(FactionsPlugin plugin) {
        super("factions");
        setAliases(List.of("factions", "faccoes", "f"));

        this.claimService = plugin.getFactionClaimService();
        this.inviteService = plugin.getFactionInviteService();
        this.lifecycleService = plugin.getFactionLifecycleService();
        this.membershipService = plugin.getFactionMembershipService();
        this.queryService = plugin.getFactionQueryService();
        this.statsService = plugin.getFactionStatsService();
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cVocê precisa ser um jogador para executar esse comando.");
            return false;
        }

        if (args.length == 0) {
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
