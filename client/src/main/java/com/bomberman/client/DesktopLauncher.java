package com.bomberman.client;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

public final class DesktopLauncher {

    private DesktopLauncher() {
    }

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("Bomberman Online Mini");
        configuration.setWindowedMode(960, 540);
        configuration.useVsync(true);
        configuration.setForegroundFPS(60);

        new Lwjgl3Application(new BombermanClient(), configuration);
    }
}
