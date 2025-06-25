package dev.balafini.factions.command;

import dev.balafini.factions.FactionsPlugin;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class FactionCommandArgument extends FactionCommand {

    private final String name;
    private final Set<String> aliasesSet;

    public FactionCommandArgument(String name, String... aliases) {
        super(FactionsPlugin.getInstance());
        this.name = name;
        this.aliasesSet = new HashSet<>(List.of(aliases));
    }

    public boolean onArgument(@NotNull Player player, String[] args) {
        return true;
    }

    public List<String> onTabComplete(@NotNull Player player, String[] args) {
        return Collections.emptyList();
    }

    public boolean matchArgument(String argument) {
        String lowerCaseArgument = argument.toLowerCase();
        return aliasesSet.contains(lowerCaseArgument) || name.equalsIgnoreCase(lowerCaseArgument);
    }

}

