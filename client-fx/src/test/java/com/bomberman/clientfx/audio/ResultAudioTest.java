package com.bomberman.clientfx.audio;

import com.bomberman.clientfx.state.UserPreferences;
import com.bomberman.common.enums.GameResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResultAudioTest {

    private final Preferences node = Preferences.userRoot().node("com/bomberman/result-audio-test-" + UUID.randomUUID());
    private final RecordingOutput output = new RecordingOutput();
    private final AtomicLong clock = new AtomicLong(1_000_000_000L);
    private final AudioManager manager = new AudioManager(output, new UserPreferences(node), clock::get);
    private final ResultAudio resultAudio = new ResultAudio(manager);

    @AfterEach
    void cleanUp() throws BackingStoreException {
        manager.close();
        node.removeNode();
    }

    @Test
    void sameResultRenderIsAnnouncedOnlyOnce() {
        resultAudio.announce(4, GameResult.WIN);
        clock.addAndGet(2_000_000_000L);
        resultAudio.announce(4, GameResult.WIN);

        assertEquals(List.of(AudioCue.VICTORY), output.cues);
    }

    @Test
    void identicalOutcomeInANewMatchIsAnnouncedAgain() {
        resultAudio.announce(4, GameResult.LOSS);
        clock.addAndGet(2_000_000_000L);
        resultAudio.announce(5, GameResult.LOSS);

        assertEquals(List.of(AudioCue.DEFEAT, AudioCue.DEFEAT), output.cues);
    }

    @Test
    void mapsEveryResultToItsStinger() {
        resultAudio.announce(1, GameResult.WIN);
        resultAudio.announce(1, GameResult.LOSS);
        resultAudio.announce(1, GameResult.DRAW);

        assertEquals(List.of(AudioCue.VICTORY, AudioCue.DEFEAT, AudioCue.DRAW), output.cues);
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
