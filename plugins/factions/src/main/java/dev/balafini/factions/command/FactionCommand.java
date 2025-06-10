package dev.balafini.factions.command;

import dev.balafini.factions.model.Faction;
import dev.balafini.factions.service.FactionInviteService;
import dev.balafini.factions.service.FactionService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.incendo.cloud.annotation.specifier.Greedy;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;

import java.util.concurrent.CompletableFuture;

@Command("faction|fac|f")
public class FactionCommand {

    private final FactionService factionService;
    private final FactionInviteService inviteService;

    public FactionCommand(FactionService factionService, FactionInviteService inviteService) {
        this.factionService = factionService;
        this.inviteService = inviteService;
    }

    @Command("")
    public void onDefault() {
        // TODO: implement GUI
    }

    @Command("create <tag> <name>")
    public void onCreate(Player sender,
                         @Argument("tag") String tag,
                         @Argument("name") @Greedy String name) {
        factionService.createFaction(name, tag, sender.getUniqueId())
                .thenAccept(faction -> sender.sendMessage(Component.text("Faction created successfully!", NamedTextColor.GREEN)))
                .exceptionally(ex -> {
                    Throwable cause = ex.getCause();
                    String errorMessage = cause != null ? cause.getMessage() : ex.getMessage();
                    sender.sendMessage(Component.text("Error: " + errorMessage, NamedTextColor.RED));
                    return null;
                });
    }

    @Command("disband")
    public void onDisband(Player sender) {
        factionService.disbandFaction(null, sender.getUniqueId())
                .thenAccept(v -> sender.sendMessage(Component.text("Faction disbanded successfully!", NamedTextColor.GREEN)))
                .exceptionally(ex -> {
                    Throwable cause = ex.getCause();
                    String errorMessage = cause != null ? cause.getMessage() : ex.getMessage();
                    sender.sendMessage(Component.text("Error: " + errorMessage, NamedTextColor.RED));
                    return null;
                });
    }

    @Command("invite <player>")
    public void onInvite(Player sender, @Argument("player") Player target) {
        if (sender.equals(target)) {
            sender.sendMessage(Component.text("You cannot invite yourself.", NamedTextColor.RED));
            return;
        }

        factionService.getPlayerFaction(sender.getUniqueId())
                .thenCompose(optFaction -> {
                    if (optFaction.isEmpty()) {
                        sender.sendMessage(Component.text("You are not in a faction.", NamedTextColor.RED));
                        return CompletableFuture.completedFuture(null);
                    }

                    Faction faction = optFaction.get();
                    return inviteService.createInvite(faction.id(), sender.getUniqueId(), target.getUniqueId())
                            .thenAccept(invite -> {
                                sender.sendMessage(Component.text("Invite sent to " + target.getName(), NamedTextColor.GREEN));

                                Component inviteMessage = Component.text("You have been invited to join ", NamedTextColor.GOLD)
                                        .append(Component.text(faction.name(), NamedTextColor.YELLOW))
                                        .append(Component.text("!", NamedTextColor.GOLD))
                                        .append(Component.newline())
                                        .append(Component.text("Type ", NamedTextColor.GOLD))
                                        .append(Component.text("/f accept " + invite.inviteId(), NamedTextColor.YELLOW))
                                        .append(Component.text(" to accept.", NamedTextColor.GOLD));

                                target.sendMessage(inviteMessage);
                            });
                })
                .exceptionally(ex -> {
                    Throwable cause = ex.getCause();
                    String errorMessage = cause != null ? cause.getMessage() : ex.getMessage();
                    sender.sendMessage(Component.text("Error: " + errorMessage, NamedTextColor.RED));
                    return null;
                });
    }
}

