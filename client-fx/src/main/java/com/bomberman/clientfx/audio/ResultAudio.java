package com.bomberman.clientfx.audio;

import com.bomberman.common.enums.GameResult;

import java.util.Objects;

/** Announces each match result once, even when the result screen is rendered repeatedly. */
public final class ResultAudio {

    private record AnnouncedResult(long matchSequence, GameResult result) {
    }

    private final AudioManager audioManager;
    private AnnouncedResult announced;

    public ResultAudio(AudioManager audioManager) {
        this.audioManager = Objects.requireNonNull(audioManager, "audioManager must not be null");
    }

    public void announce(long matchSequence, GameResult result) {
        AnnouncedResult next = new AnnouncedResult(matchSequence, Objects.requireNonNull(result, "result must not be null"));
        if (next.equals(announced)) {
            return;
        }
        announced = next;
        audioManager.play(switch (result) {
            case WIN -> AudioCue.VICTORY;
            case LOSS -> AudioCue.DEFEAT;
            case DRAW -> AudioCue.DRAW;
        });
    }
}
