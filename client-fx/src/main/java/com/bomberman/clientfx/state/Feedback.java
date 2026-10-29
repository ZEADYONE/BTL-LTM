package com.bomberman.clientfx.state;

import java.util.Objects;

/** A one-off message for the player, shown as a toast coloured by its kind. */
public record Feedback(Kind kind, String message) {

    public enum Kind {
        INFO,
        SUCCESS,
        ERROR
    }

    public Feedback {
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(message, "message must not be null");
    }

    public static Feedback info(String message) {
        return new Feedback(Kind.INFO, message);
    }

    public static Feedback success(String message) {
        return new Feedback(Kind.SUCCESS, message);
    }

    public static Feedback error(String message) {
        return new Feedback(Kind.ERROR, message);
    }
}
