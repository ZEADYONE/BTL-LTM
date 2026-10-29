package com.bomberman.clientfx.ui;

import com.bomberman.clientfx.ServerRules;

import java.util.Objects;
import java.util.Optional;

/** Client-side checks that mirror the server rules, so invalid input is caught before sending. */
public final class InputValidation {

    private InputValidation() {
    }

    public static Optional<String> usernameError(String username) {
        if (username == null || username.isBlank() || username.length() > ServerRules.MAX_USERNAME_LENGTH) {
            return Optional.of("Username must be 1–" + ServerRules.MAX_USERNAME_LENGTH + " characters.");
        }
        return Optional.empty();
    }

    public static Optional<String> passwordError(String password) {
        if (password == null || password.isBlank()) {
            return Optional.of("Password is required.");
        }
        return Optional.empty();
    }

    public static Optional<String> confirmationError(String password, String confirmation) {
        if (!Objects.equals(password, confirmation)) {
            return Optional.of("Passwords do not match.");
        }
        return Optional.empty();
    }

    /** The server trims the name before checking it. */
    public static Optional<String> roomNameError(String roomName) {
        String trimmed = roomName == null ? "" : roomName.strip();
        if (trimmed.isEmpty() || trimmed.length() > ServerRules.MAX_ROOM_NAME_LENGTH) {
            return Optional.of("Room name must be 1–" + ServerRules.MAX_ROOM_NAME_LENGTH + " characters.");
        }
        return Optional.empty();
    }

    public static Optional<String> hostError(String host) {
        if (host == null || host.isBlank()) {
            return Optional.of("Host is required.");
        }
        return Optional.empty();
    }

    public static Optional<String> portError(String port) {
        try {
            int value = Integer.parseInt(port == null ? "" : port.strip());
            if (value >= 1 && value <= 65_535) {
                return Optional.empty();
            }
        } catch (NumberFormatException notANumber) {
            // Reported below.
        }
        return Optional.of("Port must be between 1 and 65535.");
    }
}
