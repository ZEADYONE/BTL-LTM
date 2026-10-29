package com.bomberman.clientfx.audio;

import java.time.Duration;

/** Short, one-shot sounds bundled with the desktop client. */
public enum AudioCue {
    UI_CLICK("/assets/audio/sfx/ui-click.wav", 0.55, 30),
    UI_SUCCESS("/assets/audio/sfx/ui-success.wav", 0.70, 150),
    UI_ERROR("/assets/audio/sfx/ui-error.wav", 0.72, 250),
    MATCH_START("/assets/audio/sfx/match-start.wav", 0.85, 500),
    BOMB_PLACE_SELF("/assets/audio/sfx/bomb-place.wav", 0.82, 60),
    BOMB_PLACE_OTHER("/assets/audio/sfx/bomb-place-soft.wav", 0.45, 60),
    EXPLOSION("/assets/audio/sfx/explosion.wav", 0.90, 100),
    PLAYER_DEFEATED("/assets/audio/sfx/player-defeated.wav", 0.88, 500),
    VICTORY("/assets/audio/sfx/victory.wav", 0.90, 1_000),
    DEFEAT("/assets/audio/sfx/defeat.wav", 0.86, 1_000),
    DRAW("/assets/audio/sfx/draw.wav", 0.86, 1_000);

    private final String resourcePath;
    private final double gain;
    private final Duration cooldown;

    AudioCue(String resourcePath, double gain, long cooldownMillis) {
        this.resourcePath = resourcePath;
        this.gain = gain;
        cooldown = Duration.ofMillis(cooldownMillis);
    }

    public String resourcePath() {
        return resourcePath;
    }

    public double gain() {
        return gain;
    }

    public Duration cooldown() {
        return cooldown;
    }
}
