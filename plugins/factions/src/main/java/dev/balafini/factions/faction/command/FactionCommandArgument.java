package dev.balafini.factions.faction.command;

import dev.balafini.factions.FactionsPlugin;
import dev.balafini.factions.faction.service.*;
import dev.balafini.factions.user.service.UserLifecycleService;
import me.saiintbrisson.minecraft.ViewFrame;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class FactionCommandArgument {

    protected FactionClaimService claimService;
    protected FactionInviteService inviteService;
    protected FactionLifecycleService lifecycleService;
    protected FactionMemberService membershipService;
    protected FactionQueryService queryService;
    protected FactionStatsService statsService;

    protected ViewFrame viewFrame;
    protected UserLifecycleService userLifecycleService;

    private final String name;
    private final Set<String> aliasesSet;

    public FactionCommandArgument(String name, String... aliases) {
        this.name = name;
        this.aliasesSet = new HashSet<>(List.of(aliases));

        final var plugin = FactionsPlugin.getInstance();

        this.claimService = plugin.getFactionClaimService();
        this.inviteService = plugin.getFactionInviteService();
        this.lifecycleService = plugin.getFactionLifecycleService();
        this.membershipService = plugin.getFactionMemberService();
        this.queryService = plugin.getFactionQueryService();
        this.statsService = plugin.getFactionStatsService();

        this.viewFrame = plugin.getViewFrame();
        this.userLifecycleService = plugin.getUserLifecycleService();
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

