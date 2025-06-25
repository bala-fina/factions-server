package dev.balafini.factions.scoreboard;

import dev.balafini.factions.FactionsPlugin;
import dev.balafini.factions.faction.service.FactionQueryService;
import dev.balafini.factions.user.service.UserLifecycleService;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ScoreboardManager {

    private final FactionsPlugin plugin;
    private final FactionQueryService factionQueryService;
    private final UserLifecycleService userLifecycleService;

    private final Map<UUID, ScoreboardHandler> activeBoards = new ConcurrentHashMap<>();

    public ScoreboardManager(FactionsPlugin plugin, FactionQueryService factionQueryService, UserLifecycleService userLifecycleService) {
        this.plugin = plugin;
        this.factionQueryService = factionQueryService;
        this.userLifecycleService = userLifecycleService;
    }

    public void addPlayer(Player player) {
        removePlayer(player);
        ScoreboardHandler handler = new ScoreboardHandler(player, plugin, userLifecycleService, factionQueryService);
        activeBoards.put(player.getUniqueId(), handler);
    }

    public void removePlayer(Player player) {
        ScoreboardHandler handler = activeBoards.remove(player.getUniqueId());
        if (handler != null) {
            handler.delete();
        }
    }

    public void shutdown() {
        activeBoards.values().forEach(ScoreboardHandler::delete);
        activeBoards.clear();
    }
}
