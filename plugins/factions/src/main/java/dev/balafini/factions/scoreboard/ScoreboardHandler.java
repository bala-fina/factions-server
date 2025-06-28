package dev.balafini.factions.scoreboard;

import dev.balafini.factions.FactionsPlugin;
import dev.balafini.factions.faction.Faction;
import dev.balafini.factions.faction.service.FactionQueryService;
import dev.balafini.factions.user.User;
import dev.balafini.factions.user.service.UserLifecycleService;
import dev.balafini.factions.util.FormatterUtil;
import fr.mrmicky.fastboard.FastBoard;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class ScoreboardHandler {

    private static final PlainTextComponentSerializer PLAIN_TEXT_SERIALIZER = PlainTextComponentSerializer.plainText();

    private final FactionsPlugin plugin;
    private final FastBoard board;
    private final BukkitTask task;
    private final UserLifecycleService userLifecycleService;
    private final FactionQueryService factionQueryService;

    private final List<String> titleFrames;
    private int titleFrameIndex = 0;

    public ScoreboardHandler(Player player, FactionsPlugin plugin, UserLifecycleService userLifecycleService, FactionQueryService factionQueryService) {
        this.plugin = plugin;
        this.board = new FastBoard(player);
        this.userLifecycleService = userLifecycleService;
        this.factionQueryService = factionQueryService;

        this.titleFrames = createScrollingFrames("§c§lFACTIONS", "§f§l", "§c§l", 10, 5);

        this.task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::update, 0L, 4L);
    }

    private void update() {
        if (board.isDeleted() || !board.getPlayer().isOnline()) {
            delete();
            return;
        }

        CompletableFuture<Optional<User>> userFuture = userLifecycleService.findUserById(board.getPlayer().getUniqueId()).toCompletableFuture();
        CompletableFuture<Optional<Faction>> factionFuture = factionQueryService.findFactionByPlayer(board.getPlayer().getUniqueId()).toCompletableFuture();

        CompletableFuture.allOf(userFuture, factionFuture).thenAccept(_ -> {
            Optional<User> optUser = userFuture.join();
            Optional<Faction> optFaction = factionFuture.join();

            String animatedTitle = getNextTitleFrame();
            List<String> lines = buildScoreboardLines(optUser, optFaction);

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!board.isDeleted()) {
                    board.updateTitle(animatedTitle);
                    board.updateLines(lines);
                }
            });
        });
    }

    private List<String> buildScoreboardLines(Optional<User> optUser, Optional<Faction> optFaction) {
        List<ScoreboardLine> template = getScoreboardTemplate(optFaction.isPresent());

        List<String> resolvedLines = new ArrayList<>();
        for (ScoreboardLine line : template) {
            resolvedLines.add(line.resolve(optUser, optFaction));
        }
        return resolvedLines;
    }

    private List<ScoreboardLine> getScoreboardTemplate(boolean hasFaction) {
        List<ScoreboardLine> lines = new ArrayList<>();

        lines.add(new ScoreboardLine("§d", null, null));
        lines.add(new ScoreboardLine("§f Rank: §bAlpha", null, null));
        lines.add(new ScoreboardLine("§f Nível: §710", null, null));

        lines.add(new ScoreboardLine("§f Poder: %user%",
                (optUser) -> optUser.map(user -> "§7" + (int) user.power() + "/" + (int) user.maxPower()).orElse("§7-/-"),
                null
        ));
        lines.add(new ScoreboardLine("§a", null, null));

        if (hasFaction) {
            lines.add(new ScoreboardLine("§7 %faction%", null, (optFaction) -> optFaction.map(f -> "[" + f.tag() + "] " + f.name()).orElse("")));
            lines.add(new ScoreboardLine("§f  Membros: %faction%", null, (optFaction) -> optFaction.map(f -> "§7" + f.memberIds().size() + "/20").orElse("")));
            lines.add(new ScoreboardLine("§f  Poder: %faction%", null, (optFaction) -> optFaction.map(f -> "§7" + (int) f.power() + "/§7" + (int) f.maxPower()).orElse("")));
            lines.add(new ScoreboardLine("§f  Terras: %faction%", null, (optFaction) -> optFaction.map(f -> "§7" + f.claimIds().size()).orElse("")));
        } else {
            lines.add(new ScoreboardLine("§c Você não tem uma facção.", null, null));
        }

        lines.add(new ScoreboardLine("§b", null, null));
        lines.add(new ScoreboardLine("§f Coins: §2$§f" + FormatterUtil.formatNumber(100000000), null, null));
        lines.add(new ScoreboardLine("§f Cash: §6⛁" + FormatterUtil.formatNumber(5000), null, null));
        lines.add(new ScoreboardLine("§c", null, null));
        lines.add(new ScoreboardLine(centerText("§cfactions.gg", 30), null, null));

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

    private List<String> createScrollingFrames(String text, String highlightFormat, String baseFormat, int startPauseTicks, int endPauseTicks) {
        List<String> frames = new ArrayList<>();
        String plainText = PlainTextComponentSerializer.plainText().serialize(
                LegacyComponentSerializer.legacySection().deserialize(text)
        );

        for (int i = 0; i < startPauseTicks; i++) {
            frames.add(text);
        }

        for (int i = 0; i < plainText.length(); i++) {
            StringBuilder frameBuilder = new StringBuilder();
            frameBuilder.append(baseFormat);
            frameBuilder.append(plainText, 0, i);
            frameBuilder.append(highlightFormat);
            frameBuilder.append(plainText.charAt(i));
            frameBuilder.append(baseFormat);
            frameBuilder.append(plainText.substring(i + 1));
            frames.add(frameBuilder.toString());
        }

        for (int i = 0; i < endPauseTicks; i++) {
            frames.add(text);
        }

        return frames;
    }

    private String centerText(String textWithLegacyCodes, int maxWidth) {
        String plainText = PlainTextComponentSerializer.plainText().serialize(
                LegacyComponentSerializer.legacySection().deserialize(textWithLegacyCodes)
        );

        if (plainText.length() >= maxWidth) {
            return textWithLegacyCodes;
        }

        int totalPadding = maxWidth - plainText.length();
        int leftPadding = totalPadding / 2;
        int rightPadding = totalPadding - leftPadding;

        return " ".repeat(leftPadding) + textWithLegacyCodes + " ".repeat(rightPadding);
    }

    private String getNextTitleFrame() {
        String animatedTitle = titleFrames.get(titleFrameIndex);
        titleFrameIndex = (titleFrameIndex + 1) % titleFrames.size();
        return animatedTitle;
    }



    private record ScoreboardLine(String text, Function<Optional<User>, String> userPlaceholder,
                                  Function<Optional<Faction>, String> factionPlaceholder) {
        public String resolve(Optional<User> user, Optional<Faction> faction) {
            String resolved = text;
            if (userPlaceholder != null) {
                resolved = resolved.replace("%user%", userPlaceholder.apply(user));
            }
            if (factionPlaceholder != null) {
                resolved = resolved.replace("%faction%", factionPlaceholder.apply(faction));
            }
            return resolved;
        }
    }
}
