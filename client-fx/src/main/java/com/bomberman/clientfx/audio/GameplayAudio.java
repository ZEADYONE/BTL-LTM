package com.bomberman.clientfx.audio;

import com.bomberman.clientfx.game.MatchTracker;

import java.util.Objects;

/** Maps authoritative snapshot deltas to one-shot gameplay sounds. */
public final class GameplayAudio {

    private final AudioManager audioManager;
    private boolean currentPlayerDefeated;

    public GameplayAudio(AudioManager audioManager) {
        this.audioManager = Objects.requireNonNull(audioManager, "audioManager must not be null");
    }

    public void accept(MatchTracker.FrameEvents events, long currentUserId) {
        Objects.requireNonNull(events, "events must not be null");
        if (events.newMatch()) {
            currentPlayerDefeated = false;
            audioManager.play(AudioCue.MATCH_START);
        }

        if (!events.newBombs().isEmpty()) {
            boolean ownBomb = events.newBombs().stream()
                    .anyMatch(bomb -> bomb.ownerUserId() == currentUserId);
            audioManager.play(ownBomb ? AudioCue.BOMB_PLACE_SELF : AudioCue.BOMB_PLACE_OTHER);
        }

        if (!events.newExplosions().isEmpty()) {
            // One cue per authoritative batch, not one cue per flame cell or bomb.
            audioManager.play(AudioCue.EXPLOSION);
        }

        if (!currentPlayerDefeated && events.eliminatedPlayers().contains(currentUserId)) {
            currentPlayerDefeated = true;
            audioManager.play(AudioCue.PLAYER_DEFEATED);
        }
    }

    public void reset() {
        currentPlayerDefeated = false;
    }
}
