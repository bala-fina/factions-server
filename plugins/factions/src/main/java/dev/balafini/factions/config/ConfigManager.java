package dev.balafini.factions.config;

import org.bukkit.configuration.file.FileConfiguration;

public record ConfigManager(
        int maxFactionSize,
        int initialPlayerPower,
        int initialMaxPlayerPower,
        int serverMaxPlayerPower,
        int powerLostOnDeath,
        int powerGainedOnKill,
        int powerGainedPerInterval,
        int powerRegenIntervalMinutes
) {
    public static ConfigManager load(FileConfiguration config) {
        return new ConfigManager(
                config.getInt("factions.max-size", 20),
                config.getInt("player.power.initial-power", 5),
                config.getInt("player.power.initial-max-power", 5),
                config.getInt("player.power.server-max", 8),
                config.getInt("player.power.lost-on-death", 1),
                config.getInt("player.power.gain-on-kill", 1),
                config.getInt("player.power.regen-amount", 1),
                config.getInt("player.power.regen-interval-minutes", 30)
        );
    }
}
