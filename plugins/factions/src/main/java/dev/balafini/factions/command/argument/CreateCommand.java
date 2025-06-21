package dev.balafini.factions.command.argument;

import dev.balafini.factions.command.FactionCommandArgument;
import dev.balafini.factions.service.faction.FactionService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public class CreateCommand extends FactionCommandArgument {

    private final FactionService factionService;

    public CreateCommand(FactionService factionService) {
        super("create", "criar");

        this.factionService = factionService;
    }

    @Override
    public boolean onArgument(@NotNull Player player, String[] args) {
        if (args.length <= 1) {
            player.sendMessage("§cUso correto: /f criar <tag> <nome> para criar uma facção.");
            return false;
        }

        factionService.createFaction(args[0], args[1], player.getUniqueId())
                .thenAccept(faction -> {
                            player.sendMessage(Component.text("Facção '", NamedTextColor.GREEN)
                                    .append(Component.text(faction.name(), NamedTextColor.AQUA))
                                    .append(Component.text("' criada com sucesso!", NamedTextColor.GREEN)));
                            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                        }
                )
                .exceptionally(ex -> {
                    player.sendMessage("§cOcorreu um erro ao criar sua facção");
                    Bukkit.getLogger().warning(ex.getMessage());
                    return null;
                });

        return true;
    }
}
