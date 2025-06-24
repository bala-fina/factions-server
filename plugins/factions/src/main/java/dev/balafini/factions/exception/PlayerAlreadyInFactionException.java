package dev.balafini.factions.exception;

import java.util.UUID;

public class PlayerAlreadyInFactionException extends FactionException {
    public PlayerAlreadyInFactionException(String message) {
        super(message);
    }
}
