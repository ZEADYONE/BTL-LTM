package com.bomberman.clientfx.audio;

import com.bomberman.clientfx.game.MatchTracker;
import com.bomberman.clientfx.state.UserPreferences;
import com.bomberman.common.dto.BombStateDto;
import com.bomberman.common.dto.ExplosionStateDto;
import com.bomberman.common.dto.PositionDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GameplayAudioTest {

    private static final long CURRENT_USER = 7L;

    private final Preferences node = Preferences.userRoot().node("com/bomberman/gameplay-audio-test-" + UUID.randomUUID());
    private final RecordingOutput output = new RecordingOutput();
    private final AtomicLong clock = new AtomicLong(1_000_000_000L);
    private final AudioManager manager = new AudioManager(output, new UserPreferences(node), clock::get);
    private final GameplayAudio gameplayAudio = new GameplayAudio(manager);

    @AfterEach
    void cleanUp() throws BackingStoreException {
        manager.close();
        node.removeNode();
    }

    @Test
    void newMatchAndOwnBombUseOneCueEach() {
        gameplayAudio.accept(events(
                true,
                List.of(bomb("other", 9), bomb("mine", CURRENT_USER)),
                List.of(),
                List.of()
        ), CURRENT_USER);

        assertEquals(List.of(AudioCue.MATCH_START, AudioCue.BOMB_PLACE_SELF), output.cues);
    }

    @Test
    void otherBombsAndExplosionsCollapsePerSnapshotBatch() {
        gameplayAudio.accept(events(
                false,
                List.of(bomb("b1", 8), bomb("b2", 9)),
                List.of(explosion(1, 1), explosion(3, 3)),
                List.of()
        ), CURRENT_USER);

        assertEquals(List.of(AudioCue.BOMB_PLACE_OTHER, AudioCue.EXPLOSION), output.cues);
    }

    @Test
    void currentPlayerDefeatPlaysOnceUntilReset() {
        MatchTracker.FrameEvents defeated = events(false, List.of(), List.of(), List.of(CURRENT_USER, 9L));

        gameplayAudio.accept(defeated, CURRENT_USER);
        clock.addAndGet(2_000_000_000L);
        gameplayAudio.accept(defeated, CURRENT_USER);
        gameplayAudio.reset();
        clock.addAndGet(2_000_000_000L);
        gameplayAudio.accept(defeated, CURRENT_USER);

        assertEquals(List.of(AudioCue.PLAYER_DEFEATED, AudioCue.PLAYER_DEFEATED), output.cues);
    }

    @Test
    void eliminationOfAnotherPlayerIsSilent() {
        gameplayAudio.accept(events(false, List.of(), List.of(), List.of(8L, 9L)), CURRENT_USER);

        assertEquals(List.of(), output.cues);
    }

    private static MatchTracker.FrameEvents events(
            boolean newMatch,
            List<BombStateDto> bombs,
            List<ExplosionStateDto> explosions,
            List<Long> eliminatedPlayers
    ) {
        return new MatchTracker.FrameEvents(
                newMatch, List.of(), explosions, bombs, List.of(), eliminatedPlayers);
    }

    private static BombStateDto bomb(String id, long owner) {
        return new BombStateDto(id, owner, new PositionDto(1, 1), 2, 2_000);
    }

    private static ExplosionStateDto explosion(int column, int row) {
        PositionDto origin = new PositionDto(column, row);
        return new ExplosionStateDto(origin, List.of(origin), 500);
    }

    private static final class RecordingOutput implements AudioOutput {
        private final List<AudioCue> cues = new ArrayList<>();

        @Override
        public void play(AudioCue cue, double volume) {
            cues.add(cue);
        }

        @Override
        public void playMusic(MusicTrack track, double volume) {
        }

        @Override
        public void setMusicVolume(double volume) {
        }

        @Override
        public void stopMusic() {
        }

        @Override
        public void close() {
        }
    }
}
