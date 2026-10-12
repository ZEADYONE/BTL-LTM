package com.bomberman.clientfx.audio;

/** Looping background tracks bundled with the desktop client. */
public enum MusicTrack {
    MENU("/assets/audio/music/menu-loop.mp3"),
    BATTLE("/assets/audio/music/battle-loop.mp3");

    private final String resourcePath;

    MusicTrack(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    public String resourcePath() {
        return resourcePath;
    }
}
