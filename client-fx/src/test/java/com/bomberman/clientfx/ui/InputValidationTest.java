package com.bomberman.clientfx.ui;

import org.junit.jupiter.api.Test;

import static com.bomberman.clientfx.ui.InputValidation.passwordError;
import static com.bomberman.clientfx.ui.InputValidation.usernameError;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputValidationTest {

    @Test
    void usernameMustBeOneToFiftyNonBlankCharacters() {
        assertTrue(usernameError("").isPresent());
        assertTrue(usernameError("   ").isPresent());
        assertTrue(usernameError(null).isPresent());
        assertTrue(usernameError("a").isEmpty());
        assertTrue(usernameError("Nguyễn Văn Đức").isEmpty());
        assertTrue(usernameError("x".repeat(50)).isEmpty());
        assertTrue(usernameError("x".repeat(51)).isPresent());
    }

    @Test
    void passwordMustNotBeBlank() {
        assertTrue(passwordError("").isPresent());
        assertTrue(passwordError(" \t").isPresent());
        assertTrue(passwordError("secret").isEmpty());
    }
}
