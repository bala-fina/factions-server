package dev.balafini.factions.command;

import dev.balafini.factions.model.Faction;
import dev.balafini.factions.service.FactionService;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@NullMarked
@SuppressWarnings("UnstableApiUsage")
public class FactionCommand implements BasicCommand {

    private final FactionService factionService;

    public FactionCommand(FactionService factionService) {
        this.factionService = factionService;
    }


    @Override
    public void execute(CommandSourceStack commandSourceStack, String[] args) {
        if (!(commandSourceStack instanceof Player player)) {
            commandSourceStack.getSender().sendMessage(
                    Component.text("Only players can use faction commands!", NamedTextColor.RED));
            return;
        }

        if (args.length == 0) {
            showHelp(player);
            return;
        }

        String subcommand = args[0].toLowerCase();

        switch (subcommand) {
            case "create" -> handleCreateFaction(player, args);
            case "disband" -> handleDisband(player);
            case "info" -> handleInfo(player, args);
            default -> showHelp(player);
        }
    }

    @Override
    public Collection<String> suggest(CommandSourceStack stack, String[] args) {
        if (args.length == 1) {
            return List.of("create", "disband", "info");
        }
        return List.of();
    }

    private void handleCreateFaction(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(Component.text("Usage: /f create <name> <tag>", NamedTextColor.RED));
            return;
        }

        Optional<Faction> existingFaction = factionService.getPlayerFaction(player.getUniqueId());
        if (existingFaction.isPresent()) {
            player.sendMessage(Component.text("You are already in a faction!", NamedTextColor.RED));
            return;
        }

        String name = args[1];
        String tag = args[2];

        try {
            Faction faction = factionService.createFaction(name, tag, player.getUniqueId());
            player.sendMessage(Component.text("Faction created: " + faction.name() + " (" + faction.tag() + ")", NamedTextColor.GREEN));
        } catch (IllegalArgumentException e) {
            player.sendMessage(Component.text(e.getMessage(), NamedTextColor.RED));
        }
    }

    private void handleDisband(Player player) {
        Optional<Faction> factionOpt = factionService.getPlayerFaction(player.getUniqueId());
        if (factionOpt.isEmpty()) {
            player.sendMessage(Component.text("You are not in a faction!", NamedTextColor.RED));
            return;
        }

        try {
            Faction faction = factionOpt.get();
            factionService.disbandFaction(faction.id(), player.getUniqueId());
            player.sendMessage(Component.text("Faction disbanded successfully!", NamedTextColor.GREEN));
        } catch (IllegalArgumentException e) {
            player.sendMessage(Component.text(e.getMessage(), NamedTextColor.RED));
        }
    }

    private void handleInfo(Player player, String[] args) {
        Faction faction;

        if (args.length > 1) {
            String query = args[1];
            Optional<Faction> factionOpt = factionService.getFactionByName(query);

            if (factionOpt.isEmpty()) {
                factionOpt = factionService.getFactionByTag(query);
            }

            if (factionOpt.isEmpty()) {
                player.sendMessage(Component.text("Faction not found!", NamedTextColor.RED));
                return;
            }
            faction = factionOpt.get();
        } else {
            Optional<Faction> factionOpt = factionService.getPlayerFaction(player.getUniqueId());
            if (factionOpt.isEmpty()) {
                player.sendMessage(Component.text("You are not in a faction!", NamedTextColor.RED));
                return;
            }
            faction = factionOpt.get();
        }

        player.sendMessage(Component.text("--- Faction Info ---", NamedTextColor.GREEN));
        player.sendMessage(Component.text("Name: " + faction.name(), NamedTextColor.GREEN));
        player.sendMessage(Component.text("Tag: [" + faction.tag() + "]", NamedTextColor.GREEN));
        player.sendMessage(Component.text("Members (" + faction.members().size() + "):", NamedTextColor.GREEN));

        faction.members().forEach(member -> {
            String playerName = player.getServer().getOfflinePlayer(member.playerId()).getName();
            player.sendMessage(Component.text("  " + member.role().getPrefix() + " " + playerName + " (" + member.role().getName() + ")", NamedTextColor.GREEN));
        });
    }

    private void showHelp(Player player){
        player.sendMessage(Component.text("/f create <name> <tag> - Create a new faction", NamedTextColor.WHITE));
        player.sendMessage(Component.text("/f disband - Disband your faction", NamedTextColor.WHITE));
        player.sendMessage(Component.text("/f info [name/tag] - Show faction information", NamedTextColor.WHITE));
    }
}

