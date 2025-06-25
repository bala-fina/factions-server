package dev.balafini.factions.command;

import dev.balafini.factions.FactionsPlugin;
import dev.balafini.factions.faction.service.FactionInviteService;
import dev.balafini.factions.faction.service.FactionLifecycleService;
import dev.balafini.factions.faction.service.FactionMembershipService;
import dev.balafini.factions.faction.service.FactionQueryService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.context.CommandContext;

import java.util.List;
import java.util.stream.Collectors;

public class FactionCommand {

    private final FactionsPlugin plugin;
    private final FactionLifecycleService factionLifecycleService;
    private final FactionMembershipService factionMembershipService;
    private final FactionInviteService factionInviteService;
    private final FactionQueryService factionQueryService;

    public FactionCommand(FactionsPlugin plugin, FactionLifecycleService factionLifecycleService, FactionMembershipService factionMembershipService, FactionInviteService factionInviteService, FactionQueryService factionQueryService) {
        this.plugin = plugin;
        this.factionLifecycleService = factionLifecycleService;
        this.factionMembershipService = factionMembershipService;
        this.factionInviteService = factionInviteService;
        this.factionQueryService = factionQueryService;
    }

    @Suggestions("players")
    public List<String> playerSuggestions(final CommandContext<CommandSender> context, final String input) {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toList());
    }

    @Command("faction|factions|f|fac")
    @CommandDescription("Mostra informações sobre sua facção.")
    public void onDefault(final Player player) {
        factionQueryService.findFactionByPlayer(player.getUniqueId())
                .thenAccept(optFaction -> {
                    if (optFaction.isEmpty()) {
                        player.sendMessage("§cVocê não pertence a nenhuma facção.");
                        return;
                    }
                    player.sendMessage("§aMostrando informações da facção: " + optFaction.get().name());
                });
    }

    @Command("faction|factions|f|fac create|criar <tag> <name>")
    public void onCreate(
            final Player player,
            @Argument("tag") final String tag,
            @Argument(value = "name", parserName = "greedy") final String name) {

        factionLifecycleService.createFaction(tag, name, player.getUniqueId(), player.getName())
                .thenAccept(faction -> player.sendMessage("§aFacção criada com sucesso: [" + faction.tag() + "] " + faction.name()));
    }

    @Command("faction|factions|f|fac disband|desfazer")
    public void onDisband(final Player player) {
        factionLifecycleService.disbandFaction(player.getUniqueId())
                .thenAccept(_ -> player.sendMessage("§aSua facção foi desfeita com sucesso."));
    }

    @Command("faction|factions|f|fac leave|sair")
    public void onLeave(final Player player) {
        factionMembershipService.leaveFaction(player.getUniqueId())
                .thenAccept(_ -> player.sendMessage("§aVocê saiu da sua facção."));
    }

    @Command("faction|factions|f|fac kick|expulsar <player>")
    public void onKick(
            final Player player,
            @Argument(value = "player", suggestions = "players") final Player target) {

        factionMembershipService.kickMember(player.getUniqueId(), target.getUniqueId())
                .thenAccept(_ -> player.sendMessage("§aVocê expulsou " + target.getName() + " da facção."));
    }

    @Command("faction|factions|f|fac invite|convidar <player>")
    public void onInvite(
            final Player player,
            @Argument(value = "player", suggestions = "players") final Player target) {

        factionInviteService.createInvite(player.getUniqueId(), target.getUniqueId())
                .thenAccept(invite -> {
                    player.sendMessage("§aConvite enviado para " + target.getName() + " para a facção [" + invite.factionTag() + "]");
                    target.sendMessage("§aVocê foi convidado para a facção [" + invite.factionTag() + "] por " + player.getName() + ". Use /f accept " + invite.factionTag() + " para aceitar.");
                });

    }

    @Command("faction|factions|f|fac accept|aceitar <tag>")
    public void onAccept(
            final Player player,
            @Argument("tag") final String factionTag) {

        factionInviteService.acceptInvite(player.getUniqueId(), player.getName(), factionTag)
                .thenAccept(faction -> player.sendMessage("§aVocê aceitou o convite para a facção: [" + faction.tag() + "] " + faction.name()));
    }

    @Command("faction|factions|f|fac deny|recusar <tag>")
    public void onDeny(
            final Player player,
            @Argument("tag") final String factionTag) {

        factionInviteService.denyInvite(player.getUniqueId(), factionTag)
                .thenAccept(_ -> player.sendMessage("§aVocê recusou o convite para a facção: " + factionTag));
    }

}
