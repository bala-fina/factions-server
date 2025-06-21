package dev.balafini.factions.scoreboard;

import dev.balafini.factions.FactionsPlugin;
import dev.balafini.factions.service.faction.FactionService;
import dev.balafini.factions.service.user.UserService;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ScoreboardManager {

    private final FactionsPlugin plugin;
    private final FactionService factionService;
    private final UserService userService;

    private final Map<UUID, ScoreboardHandler> activeBoards = new ConcurrentHashMap<>();

    public ScoreboardManager(FactionsPlugin plugin, FactionService factionService, UserService userService) {
        this.plugin = plugin;
        this.factionService = factionService;
        this.userService = userService;
    }

    public void addPlayer(Player player) {
        removePlayer(player);
        ScoreboardHandler handler = new ScoreboardHandler(player, plugin, factionService, userService);
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
