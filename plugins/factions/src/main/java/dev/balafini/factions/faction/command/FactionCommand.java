package dev.balafini.factions.faction.command;

import com.google.common.collect.ImmutableMap;
import dev.balafini.factions.FactionsPlugin;
import dev.balafini.factions.faction.command.arguments.*;
import dev.balafini.factions.faction.service.FactionQueryService;
import dev.balafini.factions.user.service.UserLifecycleService;
import dev.balafini.factions.user.view.UserMainView;
import me.saiintbrisson.minecraft.ViewFrame;
import org.bukkit.Bukkit;
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

    private final FactionsPlugin plugin;
    private final FactionQueryService factionQueryService;
    private final UserLifecycleService userLifecycleService;
    private final ViewFrame viewFrame;

    public FactionCommand(FactionsPlugin plugin, FactionQueryService factionQueryService, UserLifecycleService userLifecycleService, ViewFrame viewFrame) {
        super("factions");
        setAliases(List.of("factions", "faccoes", "f"));

        this.plugin = plugin;
        this.factionQueryService = factionQueryService;
        this.userLifecycleService = userLifecycleService;
        this.viewFrame = viewFrame;

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
                new InfoCommand(),
                new ClaimCommand(),
                new HelpCommand()
        ));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String label, String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cVocê precisa ser um jogador para executar esse comando.");
            return false;
        }

        if (args.length == 0) {
            return openPlayerNoFactionView(player);
        }

        FactionCommandArgument factionArgument = argumentSet.stream()
                .filter(argument -> argument.matchArgument(args[0].toLowerCase()))
                .findFirst()
                .orElse(null);

        if (factionArgument == null) {
            return openPlayerNoFactionView(player);
        }

        return factionArgument.onArgument(player, Arrays.copyOfRange(args, 1, args.length));
    }

    private boolean openPlayerNoFactionView(Player player) {
        factionQueryService.findFactionByPlayer(player.getUniqueId())
                .thenCompose(faction -> {
                    if (faction.isEmpty()) {
                        return userLifecycleService.getOrCreateUser(player.getUniqueId(), player.getName())
                                .thenAccept(user -> Bukkit.getScheduler().runTask(plugin, () ->
                                        viewFrame.open(UserMainView.class, player, ImmutableMap.of(
                                                "user", user
                                        ))));
                    }
                    // TODO: Open faction main view
                    return null;
                });
        return true;
    }



    /*
       Implemented commands:
            create, disband, leave, invite, accept, deny, demote, promote, kick, info
       Remaining commands to implement:
             list, claim
     */

}
