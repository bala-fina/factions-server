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
import org.incendo.cloud.annotations.Commands;

import java.time.Instant;
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
    public void onDefault(Player player) {
        factionService.findFactionByPlayer(player.getUniqueId()).thenAccept(optFaction -> {
            if (optFaction.isPresent()) {
                displayFactionInfo(player, optFaction.get());
            } else {
                player.sendMessage(Component.text("Você não está em uma facção. Use /f create <tag> <nome> para criar uma.", NamedTextColor.RED));
            }
        }).exceptionally(ex -> handleException(player, ex));
    }

    @Command("create|criar <tag> <name>")
    public void onCreate(Player player, @Argument("tag") String tag, @Argument("name") @Greedy String name) {
        factionService.createFaction(name, tag, player.getUniqueId())
                .thenAccept(faction -> player.sendMessage(Component.text("Facção '", NamedTextColor.GREEN)
                        .append(Component.text(faction.name(), NamedTextColor.AQUA))
                        .append(Component.text("' criada com sucesso!"))))
                .exceptionally(ex -> handleException(player, ex));
    }

    @Command("disband")
    public void onDisband(Player player) {
        factionService.findFactionByPlayer(player.getUniqueId()).thenCompose(optFaction -> {
            if (optFaction.isEmpty()) {
                player.sendMessage(Component.text("Você não está em uma facção.", NamedTextColor.RED));
                return CompletableFuture.completedFuture(null);
            }
            return factionService.disbandFaction(optFaction.get().factionId(), player.getUniqueId())
                    .thenAccept(v -> player.sendMessage(Component.text("Facção desbandada com sucesso!", NamedTextColor.GREEN)));
        }).exceptionally(ex -> handleException(player, ex));
    }

    @Command("invite|invitar|convidar <player>")
    public void onInvite(Player player, @Argument("player") Player target) {
        if (player.equals(target)) {
            player.sendMessage(Component.text("Você não pode convidar a si mesmo.", NamedTextColor.RED));
            return;
        }

        factionService.findFactionByPlayer(player.getUniqueId()).thenCompose(optFaction -> {
            if (optFaction.isEmpty()) {
                return CompletableFuture.failedFuture(new IllegalStateException("Você precisa estar em uma facção para convidar jogadores."));
            }
            Faction faction = optFaction.get();
            return inviteService.createInvite(faction.tag(), player.getUniqueId(), target.getUniqueId());
        }).thenAccept(invite -> {
            player.sendMessage(Component.text("Convite enviado para " + target.getName(), NamedTextColor.GREEN));
            target.sendMessage(Component.text("Você foi convidado para a facção '" + invite.factionTag() + "'.", NamedTextColor.GOLD));
            target.sendMessage(Component.text("Digite ", NamedTextColor.GRAY)
                    .append(Component.text("/f accept " + invite.factionTag(), NamedTextColor.YELLOW))
                    .append(Component.text(" para aceitar.", NamedTextColor.GRAY)));
        }).exceptionally(ex -> handleException(player, ex));
    }

    @Command("accept|aceitar|join <tag>")
    public void onAccept(Player player, @Argument(value = "tag") String factionTag) {
        inviteService.acceptInvite(player.getUniqueId(), factionTag)
                .thenAccept(faction -> player.sendMessage(Component.text("Você entrou na facção: ", NamedTextColor.GREEN)
                        .append(Component.text(faction.name(), NamedTextColor.AQUA))))
                .exceptionally(ex -> handleException(player, ex));
    }

    @Command("deny|negar|rejeitar <tag>")
    public void onDeny(Player player, @Argument("tag") String factionTag) {
        inviteService.denyInvite(player.getUniqueId(), factionTag)
                .thenAccept(v -> player.sendMessage(Component.text("Você recusou o convite para a facção: " + factionTag, NamedTextColor.GREEN)))
                .exceptionally(ex -> handleException(player, ex));
    }

    @Command("info|ver <tag>")
    public void onInfo(CommandSender sender, @Argument(value = "tag") String factionTag) {
        if (factionTag == null) {
            if (sender instanceof Player player) {
                onDefault(player); // If no tag, show player's own faction
            } else {
                sender.sendMessage(Component.text("Por favor, especifique a tag de uma facção.", NamedTextColor.RED));
            }
            return;
        }

        factionService.findFactionByTag(factionTag).thenAccept(optFaction -> {
            if (optFaction.isPresent()) {
                displayFactionInfo(sender, optFaction.get());
            } else {
                sender.sendMessage(Component.text("Facção com a tag '" + factionTag + "' não encontrada.", NamedTextColor.RED));
            }
        }).exceptionally(ex -> handleException(sender, ex));
    }

    @Command("leave")
    public void onLeave(Player player) {
        factionService.removeMember(null, player.getUniqueId(), player.getUniqueId())
                .thenAccept(v -> player.sendMessage(Component.text("Você saiu da sua facção.", NamedTextColor.GREEN)))
                .exceptionally(ex -> handleException(player, ex));
    }

    @Command("kick <player>")
    public void onKick(Player player, @Argument("player") Player target) {
        factionService.findFactionByPlayer(player.getUniqueId()).thenCompose(optFaction -> {
            if (optFaction.isEmpty()) {
                return CompletableFuture.failedFuture(new IllegalStateException("Você não está em uma facção."));
            }
            return factionService.removeMember(optFaction.get().factionId(), player.getUniqueId(), target.getUniqueId());
        }).thenAccept(v -> {
            player.sendMessage(Component.text(target.getName() + " foi expulso da facção.", NamedTextColor.GREEN));
            target.sendMessage(Component.text("Você foi expulso da sua facção.", NamedTextColor.RED));
        }).exceptionally(ex -> handleException(player, ex));
    }

    private record FactionDisplayData(
            Component header, Component leader, Component membersCount,
            List<Component> onlineMembers, List<Component> offlineMembers,
            Component createdAt
    ) {}

    private void displayFactionInfo(CommandSender sender, Faction faction) {
        CompletableFuture.supplyAsync(() -> {
                    // --- BACKGROUND THREAD: Safe to make blocking calls ---
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

                    String leaderName = resolvePlayerName(faction.getLeader().playerId());
                    List<Component> offlineMemberComponents = offlinePlayerIds.stream()
                            .map(uuid -> Component.text("•" + resolvePlayerName(uuid), NamedTextColor.RED))
                            .collect(Collectors.toList());
                    List<Component> onlineMemberComponents = onlinePlayers.stream()
                            .map(p -> Component.text("•" + p.getName(), NamedTextColor.GREEN))
                            .collect(Collectors.toList());

                    // --- PREPARE ALL COMPONENTS ---
                    Component header = Component.text("Informações da facção - [", NamedTextColor.GREEN)
                            .append(Component.text(faction.tag(), NamedTextColor.WHITE))
                            .append(Component.text("] " + faction.name(), NamedTextColor.GREEN));
                    Component leaderComponent = Component.text("Líder: ", NamedTextColor.WHITE).append(Component.text(leaderName));
                    Component membersCountComponent = Component.text("Membros: ", NamedTextColor.WHITE).append(Component.text(faction.members().size() + "/" + maxFactionSize));
                    Component createdAtComponent = Component.text("Criada em: ", NamedTextColor.WHITE).append(Component.text(formatDate(faction.createdAt())));

                    return new FactionDisplayData(header, leaderComponent, membersCountComponent, onlineMemberComponents, offlineMemberComponents, createdAtComponent);

                }, executor).thenAcceptAsync(data -> {
                    // --- MAIN SERVER THREAD: Safe to send messages ---
                    sender.sendMessage(data.header());
                    sender.sendMessage(data.leader());
                    sender.sendMessage(data.membersCount());

                    if (!data.onlineMembers().isEmpty()) {
                        sender.sendMessage(Component.text("Membros online (" + data.onlineMembers().size() + "):", NamedTextColor.WHITE));
                        sender.sendMessage(Component.join(JoinConfiguration.separator(Component.text(", ", NamedTextColor.GRAY)), data.onlineMembers()));
                    }
                    if (!data.offlineMembers().isEmpty()) {
                        sender.sendMessage(Component.text("Membros offline (" + data.offlineMembers().size() + "):", NamedTextColor.WHITE));
                        sender.sendMessage(Component.join(JoinConfiguration.separator(Component.text(", ", NamedTextColor.GRAY)), data.offlineMembers()));
                    }
                    sender.sendMessage(data.createdAt());

                }, runnable -> Bukkit.getScheduler().runTask(plugin, runnable))
                .exceptionally(ex -> handleException(sender, ex));
    }

    private String resolvePlayerName(UUID uuid) {
        try {
            return Optional.ofNullable(Bukkit.getOfflinePlayer(uuid).getName()).orElse("[Desconhecido]");
        } catch (Exception e) {
            return "[Erro ao carregar]";
        }
    }

    private String formatDate(Instant instant) {
        DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                .withLocale(Locale.of("pt", "BR"))
                .withZone(ZoneId.of("America/Sao_Paulo"));
        return formatter.format(instant);
    }

    private Void handleException(CommandSender sender, Throwable ex) {
        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
        sender.sendMessage(Component.text("Erro: " + cause.getMessage(), NamedTextColor.RED));
        // For server administrators, log the full error to the console
        if (!(ex instanceof IllegalStateException || ex instanceof IllegalArgumentException)) {
            plugin.getLogger().severe("An error occurred executing a faction command: " + cause);
            for (StackTraceElement element : cause.getStackTrace()) {
                plugin.getLogger().severe("    at " + element.toString());
            }
        }
        return null;
    }
}

