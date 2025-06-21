package dev.balafini.factions.command;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class FactionCommandArgument {

    private final String name;
    private final Set<String> aliases;

    public FactionCommandArgument(String name, String... aliases) {
        this.name = name;
        this.aliases = new HashSet<>(List.of(aliases));
    }

    public boolean onArgument(@NotNull Player player, String[] args) {
        return true;
    }

    public List<String> onTabComplete(@NotNull Player player, String[] args) {
        return List.of();
    }

    public boolean matchArgument(String argument) {
        String lowerCaseArgument = argument.toLowerCase();
        return aliases.contains(lowerCaseArgument) || name.equalsIgnoreCase(lowerCaseArgument);
    }
}
