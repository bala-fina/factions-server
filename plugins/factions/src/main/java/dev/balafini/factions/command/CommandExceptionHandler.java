package dev.balafini.factions.command;

import dev.balafini.factions.exception.FactionException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.annotations.exception.ExceptionHandler;
import org.incendo.cloud.exception.ArgumentParseException;
import org.incendo.cloud.exception.NoPermissionException;

import java.util.logging.Logger;

public class CommandExceptionHandler {
    private static final Logger LOGGER = Logger.getLogger(CommandExceptionHandler.class.getName());

    @ExceptionHandler(FactionException.class)
    public void handleFactionException(final CommandSender sender, final FactionException ex) {
        sender.sendMessage(Component.text("Erro: ", NamedTextColor.RED).append(Component.text(ex.getMessage(), NamedTextColor.GRAY)));
    }

    @ExceptionHandler(NoPermissionException.class)
    public void handleNoPermission(final CommandSender sender) {
        sender.sendMessage(Component.text("Você não tem permissão para usar este comando.", NamedTextColor.RED));
    }

    @ExceptionHandler(ArgumentParseException.class)
    public void handleArgumentParse(final CommandSender sender, final ArgumentParseException ex) {
        sender.sendMessage(Component.text("Argumento inválido: ", NamedTextColor.RED).append(Component.text(ex.getCause().getMessage(), NamedTextColor.GRAY)));
    }

    @ExceptionHandler(Throwable.class)
    public void handleFallback(final CommandSender sender, final Throwable throwable) {
        sender.sendMessage(Component.text("Ocorreu um erro inesperado. Contate um administrador.", NamedTextColor.RED));
        LOGGER.log(java.util.logging.Level.SEVERE, "An unexpected error occurred during command execution:", throwable);
    }
}
