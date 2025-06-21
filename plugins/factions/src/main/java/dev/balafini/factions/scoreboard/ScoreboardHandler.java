package dev.balafini.factions.scoreboard;

import dev.balafini.factions.FactionsPlugin;
import dev.balafini.factions.model.faction.Faction;
import dev.balafini.factions.model.user.User;
import dev.balafini.factions.service.faction.FactionService;
import dev.balafini.factions.service.user.UserService;
import fr.mrmicky.fastboard.FastBoard;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class ScoreboardHandler {

    private final FastBoard board;
    private final BukkitTask task;

    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();
    private static final PlainTextComponentSerializer PLAIN_TEXT_SERIALIZER = PlainTextComponentSerializer.plainText();

    public ScoreboardHandler(Player player, FactionsPlugin plugin, FactionService factionService, UserService userService) {
        this.board = new FastBoard(player);

        this.task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            update(factionService, userService);
        }, 0L, 20L);
    }

    private void update(FactionService factionService, UserService userService) {
        if (board.isDeleted() || !board.getPlayer().isOnline()) {
            delete();
            return;
        }

        CompletableFuture<Optional<User>> userFuture = userService.findUser(board.getPlayer().getUniqueId()).toCompletableFuture();
        CompletableFuture<Optional<Faction>> factionFuture = factionService.findFactionByPlayer(board.getPlayer().getUniqueId()).toCompletableFuture();

        CompletableFuture.allOf(userFuture, factionFuture).thenAccept(_ -> {
            Optional<User> optUser = userFuture.join();
            Optional<Faction> optFaction = factionFuture.join();

            List<String> lines = buildScoreboardLines(optUser, optFaction);

            board.updateTitle(centerText("§d§lPhoenix"));
            board.updateLines(lines);
        });
    }

    private List<String> buildScoreboardLines(Optional<User> optUser, Optional<Faction> optFaction) {
        List<String> lines = new ArrayList<>();

        lines.add("§fClasse: §5Witcher");
        lines.add("§fNível: §7*10");
        lines.add("§fPoder: " + optUser.map(user ->
                "§e" + (int) user.power() + "/" + (int) user.maxPower()).orElse("§e-/-"));
        lines.add("§a");

        if (optFaction.isPresent()) {
            Faction faction = optFaction.get();
            lines.add("§7[" + faction.tag() + "] " + faction.name());
            lines.add("§fMembros: §a" + faction.members().size() + "/20");
            lines.add("§fPoder: §7" + (int) faction.power() + "§f/§a" + (int) faction.maxPower());
            lines.add("§fTerras: §a0");
        } else {
            lines.add("§cVocê não tem uma facção.");
        }
        lines.add("§b");
        lines.add("§fDoces: §a" + formatNumber(1000000));
        lines.add("§dBalas: §e1,818");
        lines.add("§c");

        lines.add(centerText("§fredebalinha.com.br"));

        return lines;
    }

    public void delete() {
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
        if (!board.isDeleted()) {
            board.delete();
        }
    }

    private String centerText(String textWithLegacyCodes) {
        Component component = LEGACY_SERIALIZER.deserialize(textWithLegacyCodes);
        String plainText = PLAIN_TEXT_SERIALIZER.serialize(component);

        int maxWidth = 30;
        int textWidth = plainText.length();
        int padding = Math.max(0, (maxWidth - textWidth) / 2);

        return " ".repeat(padding) + textWithLegacyCodes;
    }

    private String formatNumber(double number) {
        if (number < 1000) return new DecimalFormat("#,###").format(number);
        if (number < 1_000_000) return String.format(Locale.US, "%.1fK", number / 1000.0).replace(".0K", "K");
        if (number < 1_000_000_000) return String.format(Locale.US, "%.1fM", number / 1_000_000.0).replace(".0M", "M");
        if (number < 1_000_000_000_000L)
            return String.format(Locale.US, "%.1fB", number / 1_000_000_000.0).replace(".0B", "B");
        return String.format(Locale.US, "%.1fT", number / 1_000_000_000_000.0).replace(".0T", "T");
    }
}
