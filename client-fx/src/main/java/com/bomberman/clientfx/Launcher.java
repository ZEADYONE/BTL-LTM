package com.bomberman.clientfx;

import javafx.application.Application;

/**
 * Entry point. JavaFX refuses to start an {@link Application} subclass directly as the main
 * class when it is loaded from the classpath, which is how Gradle and jpackage run the client.
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        Application.launch(BombermanApp.class, args);
    }
}
