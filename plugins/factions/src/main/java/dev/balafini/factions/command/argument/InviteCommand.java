package dev.balafini.factions.command.argument;

import dev.balafini.factions.command.FactionCommandArgument;
import dev.balafini.factions.service.faction.FactionInviteService;
import dev.balafini.factions.service.faction.FactionService;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class InviteCommand extends FactionCommandArgument {

    private final FactionService factionService;
    private final FactionInviteService inviteService;


    public InviteCommand(FactionService factionService, FactionInviteService inviteService) {
        super("invite", "invitar", "convidar");

        this.factionService = factionService;
        this.inviteService = inviteService;
    }

    @Override
    public boolean onArgument(@NotNull Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage("§cUso correto: /faction invite <jogador>");
            return false;
        }

        return true;
    }
}
