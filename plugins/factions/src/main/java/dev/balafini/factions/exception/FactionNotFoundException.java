package dev.balafini.factions.exception;

import java.util.UUID;

public class FactionNotFoundException extends FactionException{
    public FactionNotFoundException(String message) {
        super(message);
    }
}
