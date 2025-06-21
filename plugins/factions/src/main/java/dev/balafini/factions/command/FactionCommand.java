package dev.balafini.factions.command;

import dev.balafini.factions.FactionsPlugin;
import dev.balafini.factions.command.argument.CreateCommand;
import dev.balafini.factions.command.argument.DisbandCommand;
import dev.balafini.factions.config.ConfigManager;
import dev.balafini.factions.model.faction.Faction;
import dev.balafini.factions.service.faction.FactionService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;

@SuppressWarnings("UnstableApiUsage")
public class FactionCommand extends BukkitCommand {

    private final Set<FactionCommandArgument> arguments = new HashSet<>();

    private final FactionsPlugin plugin;
    private final FactionService factionService;
    private final ConfigManager config;
    private final ExecutorService executor;

    public FactionCommand(FactionsPlugin plugin, FactionService factionService, ConfigManager config, ExecutorService executor) {
        super("faction");
        setAliases(List.of("factions", "f", "fac"));

        this.plugin = plugin;
        this.factionService = factionService;
        this.config = config;
        this.executor = executor;

        arguments.add(new CreateCommand(factionService));
        arguments.add(new DisbandCommand(factionService));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cVocê precisa ser um jogador para executar esse comando.");
            return false;
        }

        factionService.findFactionByPlayer(player.getUniqueId())
                .thenCompose(optFaction -> {
                    if (optFaction.isEmpty()) {
                        player.sendMessage("§cVocê não pertence a nenhuma facção.");
                        return CompletableFuture.completedFuture(List.of());
                    }

                    Faction playerFaction = optFaction.get();

                    return factionService.getFactionRank(playerFaction).thenComposeAsync(rank -> {
                        return prepareInfoMessages(playerFaction, rank);
                    }, executor);

                }).thenAcceptAsync(messages -> {
                    messages.forEach(player::sendMessage);

                }, runnable -> Bukkit.getScheduler().runTask(plugin, runnable))
                .exceptionally(ex -> {
                    player.sendMessage("§cOcorreu um erro ao buscar as informações da facção.");
                    Bukkit.getLogger().warning(ex.getMessage());
                    return null;
                });


        FactionCommandArgument factionCommandArgument = arguments.stream()
                .filter(argument -> argument.matchArgument(args[0]))
                .findFirst()
                .orElse(null);

        if (factionCommandArgument == null) {
            player.sendMessage("§cComando inválido. Use /faction help para ver os comandos disponíveis.");
            return false;
        }

        return factionCommandArgument.onArgument(player, Arrays.copyOfRange(args, 1, args.length));
    }

    private CompletionStage<List<Component>> prepareInfoMessages(Faction faction, long rank) {
        return CompletableFuture.supplyAsync(() -> {
            String leaderName = resolvePlayerName(faction.getLeader().playerId());
            String kdrFormatted = String.format(Locale.US, "%.2f", faction.kdr());

            List<Component> messages = new ArrayList<>();
            messages.add(Component.text(""));
            messages.add(Component.text(" §7[§b" + faction.tag() + "§7] §b" + faction.name()));
            messages.add(Component.text(" §7Líder: §f" + leaderName));
            messages.add(Component.text(" §7Membros: §f" + faction.members().size() + " / " + config.maxFactionSize()));
            messages.add(Component.text(" §7Poder: §f" + (int) faction.power() + " / " + (int) faction.maxPower()));
            messages.add(Component.text(" §7KDR Total: §f" + kdrFormatted + " §7(Rank: §b#" + rank + "§7)"));
            messages.add(Component.text(" §7Criada em: §f" + formatDate(faction.createdAt())));
            messages.add(Component.text(""));

            return messages;
        }, executor);
    }

    private String formatDate(Instant instant) {
        DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                .withLocale(Locale.of("pt", "BR"))
                .withZone(ZoneId.of("America/Sao_Paulo"));
        return formatter.format(instant);
    }

    private String resolvePlayerName(UUID uuid) {
        try {
            return Optional.ofNullable(Bukkit.getOfflinePlayer(uuid).getName()).orElse("[Desconhecido]");
        } catch (Exception e) {
            return "[Erro ao carregar]";
        }
    }
}
