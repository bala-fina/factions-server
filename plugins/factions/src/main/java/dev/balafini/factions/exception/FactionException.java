package dev.balafini.factions.exception;

public abstract class FactionException extends RuntimeException {
    protected FactionException(String message) {
        super(message);
    }

    protected FactionException(String message, Throwable cause) {
        super(message, cause);
    }
}