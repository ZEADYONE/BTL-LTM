package com.bomberman.clientfx.ui;

import com.bomberman.clientfx.ServerRules;

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
}
