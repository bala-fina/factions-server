package dev.balafini.factions.command;

import dev.balafini.factions.FactionsPlugin;
import dev.balafini.factions.model.faction.Faction;
import dev.balafini.factions.model.faction.FactionMember;
import dev.balafini.factions.service.FactionInviteService;
import dev.balafini.factions.service.FactionService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.annotation.specifier.Greedy;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@Command("faction|fac|f")
public class FactionCommand {

    private final FactionsPlugin plugin;
    private final FactionService factionService;
    private final FactionInviteService inviteService;
    private final ExecutorService executor;
    private final int maxFactionSize;

    public FactionCommand(FactionsPlugin plugin, FactionService factionService, FactionInviteService inviteService, ExecutorService executor, int maxFactionSize) {
        this.plugin = plugin;
        this.factionService = factionService;
        this.inviteService = inviteService;
        this.executor = executor;
        this.maxFactionSize = maxFactionSize;
    }

    @Command("")
    public void onDefault(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Esse comando só pode ser usado por jogadores.", NamedTextColor.RED));
            return;
        }

        factionService.findFactionByPlayer(player.getUniqueId()).thenAccept(optFaction -> {
            if (optFaction.isEmpty()) {
                // TODO: implement GUI
                player.sendMessage(Component.text("Você não está em uma facção. Use /f create <tag> <nome> para criar uma.", NamedTextColor.RED));
                return;
            }

            displayFactionInfo(player, optFaction.get());

        }).exceptionally(ex -> handleException(sender, ex));

    }

    @Command("create|criar <tag> <name>")
    public void onCreate(CommandSender sender, @Argument("tag") String tag, @Argument("name") @Greedy String name) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Esse comando só pode ser usado por jogadores.", NamedTextColor.RED));
            return;
        }

        factionService.createFaction(name, tag, player.getUniqueId())
                .thenAccept(faction -> {
                    player.sendMessage(Component.text("Facção '", NamedTextColor.GREEN)
                            .append(Component.text(faction.name(), NamedTextColor.AQUA))
                            .append(Component.text("' criada com sucesso!")));
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
                })
                .exceptionally(ex -> handleException(player, ex));
    }

    @Command("disband")
    public void onDisband(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Esse comando só pode ser usado por jogadores.", NamedTextColor.RED));
            return;
        }

        factionService.findFactionByPlayer(player.getUniqueId()).thenCompose(optFaction -> {
            if (optFaction.isEmpty()) {
                player.sendMessage(Component.text("Você não está em uma facção. Use /f create <tag> <nome> para criar uma.", NamedTextColor.RED));
                return CompletableFuture.completedFuture(null);
            }
            return factionService.disbandFaction(optFaction.get().factionId(), player.getUniqueId())
                    .thenAccept(v -> player.sendMessage(Component.text("A facção foi desbandada com sucesso!", NamedTextColor.GREEN)));
        }).exceptionally(ex -> handleException(player, ex));


    }

    @Command("invite|invitar|convidar <player>")
    public void onInvite(CommandSender sender, @Argument("player") Player target) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Esse comando só pode ser usado por jogadores.", NamedTextColor.RED));
            return;
        }

        factionService.findFactionByPlayer(player.getUniqueId()).thenCompose(optFaction -> {
            if (optFaction.isEmpty()) {
                player.sendMessage(Component.text("Voce não está em uma facção. Use /f create <tag> <nome> para criar uma.", NamedTextColor.RED));
                return CompletableFuture.completedFuture(null);
            }
            Faction faction = optFaction.get();
            return inviteService.createInvite(faction.tag(), player.getUniqueId(), target.getUniqueId())
                    .thenAccept(invite -> {
                        player.sendMessage(Component.text("Convite enviado para " + target.getName(), NamedTextColor.GREEN));
                        target.sendMessage(Component.text("Você foi convidado para a entrar na facção '" + faction.name() + "'.", NamedTextColor.GOLD));
                        target.sendMessage(Component.text("Digite ", NamedTextColor.GRAY)
                                .append(Component.text("/f aceitar" + faction.tag(), NamedTextColor.YELLOW))
                                .append(Component.text(" para entrar.", NamedTextColor.GRAY)));
                        target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
                    });
        }).exceptionally(ex -> handleException(player, ex));
    }

    @Command("accept|aceitar|join <tag>")
    public void onAccept(Player player, @Argument("tag") String factionTag) {
        inviteService.acceptInvite(player.getUniqueId(), factionTag)
                .thenAccept(faction -> {
                    player.sendMessage(Component.text("Você entrou na facção: ", NamedTextColor.GREEN)
                            .append(Component.text(faction.name(), NamedTextColor.AQUA)));
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
                    // TODO: sent to faction chat message
                })
                .exceptionally(ex -> handleException(player, ex));
    }

    @Command("deny|negar|rejeitar <tag>")
    public void onDeny(Player player, @Argument("tag") String factionTag) {
        inviteService.denyInvite(player.getUniqueId(), factionTag)
                .thenAccept(v -> player.sendMessage(Component.text("Você recusou o convite para a facção: " + factionTag, NamedTextColor.GREEN)))
                .exceptionally(ex -> handleException(player, ex));
    }

    @Command("info|ver <tag>")
    public void onInfo(CommandSender sender, @Argument("tag") String factionTag) {
        factionService.findFactionByTag(factionTag).thenAccept(optFaction -> {
            if (optFaction.isPresent()) {
                displayFactionInfo(sender, optFaction.get());
            } else {
                sender.sendMessage(Component.text("Facção '" + factionTag + "' não encontrada.", NamedTextColor.RED));
            }
        }).exceptionally(ex -> handleException(sender, ex));
    }

    private record FactionDisplayData(
            Component header, Component leader, Component membersCount,
            Component onlineHeader, Component onlineMembers,
            Component offlineHeader, Component offlineMembers,
            Component createdAt
    ) {
    }

    private void displayFactionInfo(CommandSender sender, Faction faction) {
        CompletableFuture.supplyAsync(() -> {
                    List<Player> onlinePlayers = new ArrayList<>();
                    List<UUID> offlinePlayerIds = new ArrayList<>();
                    for (var member : faction.members()) {
                        Player onlinePlayer = Bukkit.getPlayer(member.playerId());
                        if (onlinePlayer != null) {
                            onlinePlayers.add(onlinePlayer);
                        } else {
                            offlinePlayerIds.add(member.playerId());
                        }
                    }

                    String leaderName = Optional.ofNullable(Bukkit.getOfflinePlayer(faction.getLeader().playerId()).getName())
                            .orElse("Desconhecido");

                    List<Component> offlineMemberComponents = offlinePlayerIds.stream()
                            .map(uuid -> {
                                String memberName = Optional.ofNullable(Bukkit.getOfflinePlayer(uuid).getName())
                                        .orElse("Desconhecido"); // Fallback for each member
                                return Component.text("•" + memberName, NamedTextColor.RED);
                            })
                            .collect(Collectors.toList());

                    Component header = Component.text("Informações da facção - [", NamedTextColor.GREEN)
                            .append(Component.text(faction.tag(), NamedTextColor.WHITE))
                            .append(Component.text("] " + faction.name() + ":", NamedTextColor.GREEN));

                    Component leaderComponent = Component.text("Líder: ", NamedTextColor.WHITE).append(Component.text(leaderName));
                    Component membersCountComponent = Component.text("Membros: ", NamedTextColor.WHITE).append(Component.text(faction.members().size() + "/" + maxFactionSize));

                    Component onlineHeaderComponent = Component.text("Membros online: " + onlinePlayers.size(), NamedTextColor.WHITE);
                    Component onlineMembersComponent = Component.join(
                            JoinConfiguration.separator(Component.text(", ", NamedTextColor.GRAY)),
                            onlinePlayers.stream().map(p -> Component.text("•" + p.getName(), NamedTextColor.GREEN)).collect(Collectors.toList())
                    );

                    Component offlineHeaderComponent = Component.text("Membros offline: " + offlinePlayerIds.size(), NamedTextColor.WHITE);
                    Component offlineMembersComponent = Component.join(
                            JoinConfiguration.separator(Component.text(", ", NamedTextColor.GRAY)),
                            offlineMemberComponents
                    );

                    DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                            .withLocale(Locale.of("pt", "BR"))
                            .withZone(ZoneId.systemDefault());
                    Component createdAtComponent = Component.text("Criada em: ", NamedTextColor.WHITE).append(Component.text(formatter.format(faction.createdAt())));

                    return new FactionDisplayData(header, leaderComponent, membersCountComponent, onlineHeaderComponent, onlineMembersComponent, offlineHeaderComponent, offlineMembersComponent, createdAtComponent);

                }, executor).thenAcceptAsync(data -> {
                    sender.sendMessage(data.header());
                    sender.sendMessage(data.leader());
                    sender.sendMessage(data.membersCount());
                    if (!data.onlineMembers().children().isEmpty()) {
                        sender.sendMessage(data.onlineHeader());
                        sender.sendMessage(data.onlineMembers());
                    }
                    if (!data.offlineMembers().children().isEmpty()) {
                        sender.sendMessage(data.offlineHeader());
                        sender.sendMessage(data.offlineMembers());
                    }
                    sender.sendMessage(data.createdAt());

                }, runnable -> Bukkit.getScheduler().runTask(plugin, runnable))
                .exceptionally(ex -> handleException(sender, ex));
    }


    private Void handleException(CommandSender sender, Throwable ex) {
        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
        sender.sendMessage(Component.text("Error: " + cause.getMessage(), NamedTextColor.RED));
        return null;
    }
}

