package com.bomberman.clientfx.ui;

import org.junit.jupiter.api.Test;

import static com.bomberman.clientfx.ui.InputValidation.confirmationError;
import static com.bomberman.clientfx.ui.InputValidation.hostError;
import static com.bomberman.clientfx.ui.InputValidation.passwordError;
import static com.bomberman.clientfx.ui.InputValidation.portError;
import static com.bomberman.clientfx.ui.InputValidation.roomNameError;
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
    void confirmationMustMatchExactly() {
        assertTrue(confirmationError("secret", "secret").isEmpty());
        assertTrue(confirmationError("secret", "Secret").isPresent());
        assertTrue(confirmationError("secret", "secret ").isPresent());
        assertTrue(confirmationError("secret", "").isPresent());
    }

    @Test
    void serverAddressFieldsAreChecked() {
        assertTrue(hostError("192.168.1.20").isEmpty());
        assertTrue(hostError("  ").isPresent());
        assertTrue(portError("8081").isEmpty());
        assertTrue(portError(" 1 ").isEmpty());
        assertTrue(portError("65535").isEmpty());
        assertTrue(portError("0").isPresent());
        assertTrue(portError("65536").isPresent());
        assertTrue(portError("abc").isPresent());
        assertTrue(portError("").isPresent());
    }

    @Test
    void roomNameIsTrimmedThenLimitedToSixty() {
        assertTrue(roomNameError("Bomber Party!").isEmpty());
        assertTrue(roomNameError("  Phòng của Đức  ").isEmpty());
        assertTrue(roomNameError("   ").isPresent());
        assertTrue(roomNameError("x".repeat(60)).isEmpty());
        assertTrue(roomNameError("  " + "x".repeat(60) + "  ").isEmpty());
        assertTrue(roomNameError("x".repeat(61)).isPresent());
    }

    @Test
    void passwordMustNotBeBlank() {
        assertTrue(passwordError("").isPresent());
        assertTrue(passwordError(" \t").isPresent());
        assertTrue(passwordError("secret").isEmpty());
    }
}
