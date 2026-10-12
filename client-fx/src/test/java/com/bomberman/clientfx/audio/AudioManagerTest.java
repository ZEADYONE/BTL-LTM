package com.bomberman.clientfx.audio;

import com.bomberman.clientfx.state.Feedback;
import com.bomberman.clientfx.state.UserPreferences;
import com.bomberman.clientfx.ui.ScreenId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AudioManagerTest {

    private final Preferences node = Preferences.userRoot().node("com/bomberman/audio-test-" + UUID.randomUUID());
    private final UserPreferences preferences = new UserPreferences(node);
    private final FakeAudioOutput output = new FakeAudioOutput();
    private final AtomicLong clock = new AtomicLong(1_000_000_000L);
    private final AudioManager manager = new AudioManager(output, preferences, clock::get);

    @AfterEach
    void removeNode() throws BackingStoreException {
        manager.close();
        node.removeNode();
    }

    @Test
    void appliesMasterSfxAndCueGain() {
        manager.play(AudioCue.UI_CLICK);

        assertEquals(List.of(AudioCue.UI_CLICK), output.cues);
        assertEquals(0.80 * 0.80 * AudioCue.UI_CLICK.gain(), output.cueVolumes.getFirst(), 0.000_001);
    }

    @Test
    void cooldownSuppressesOnlyRapidDuplicates() {
        manager.play(AudioCue.UI_ERROR);
        manager.play(AudioCue.UI_ERROR);
        manager.play(AudioCue.UI_SUCCESS);
        clock.addAndGet(AudioCue.UI_ERROR.cooldown().toNanos());
        manager.play(AudioCue.UI_ERROR);

        assertEquals(List.of(AudioCue.UI_ERROR, AudioCue.UI_SUCCESS, AudioCue.UI_ERROR), output.cues);
    }

    @Test
    void muteIsImmediateAndPreservesVolumeSettings() {
        manager.onScreenChanged(ScreenId.HOME);
        manager.setMuted(true);
        manager.setMasterVolume(0.60);
        manager.setMusicVolume(0.25);
        manager.setSfxVolume(0.70);
        manager.play(AudioCue.UI_CLICK);

        assertTrue(manager.muted());
        assertEquals(0.60, manager.masterVolume());
        assertEquals(0.25, manager.musicVolume());
        assertEquals(0.70, manager.sfxVolume());
        assertEquals(0.0, output.musicVolume, 0.0);
        assertTrue(output.cues.isEmpty());

        manager.setMuted(false);

        assertFalse(manager.muted());
        assertEquals(0.15, output.musicVolume, 0.000_001);
        assertEquals(0.60, preferences.audioMasterVolume());
        assertEquals(0.25, preferences.audioMusicVolume());
        assertEquals(0.70, preferences.audioSfxVolume());
    }

    @Test
    void mapsScreensToMenuBattleAndSilence() {
        manager.onScreenChanged(ScreenId.LOGIN);
        manager.onScreenChanged(ScreenId.ROOM_LOBBY);
        manager.onScreenChanged(ScreenId.GAME);
        manager.onScreenChanged(ScreenId.RESULT);

        assertEquals(List.of(MusicTrack.MENU, MusicTrack.BATTLE), output.tracks);
        assertEquals(1, output.stopMusicCalls);
    }

    @Test
    void feedbackPlaysOnlySuccessAndErrorCues() {
        manager.playFeedback(Feedback.info("Info"));
        manager.playFeedback(Feedback.success("Done"));
        manager.playFeedback(Feedback.error("Failed"));

        assertEquals(List.of(AudioCue.UI_SUCCESS, AudioCue.UI_ERROR), output.cues);
    }

    @Test
    void closeIsIdempotent() {
        manager.close();
        manager.close();
        manager.play(AudioCue.UI_CLICK);

        assertEquals(1, output.closeCalls);
        assertTrue(output.cues.isEmpty());
    }

    private static final class FakeAudioOutput implements AudioOutput {
        private final List<AudioCue> cues = new ArrayList<>();
        private final List<Double> cueVolumes = new ArrayList<>();
        private final List<MusicTrack> tracks = new ArrayList<>();
        private double musicVolume;
        private int stopMusicCalls;
        private int closeCalls;

        @Override
        public void play(AudioCue cue, double volume) {
            cues.add(cue);
            cueVolumes.add(volume);
        }

        @Override
        public void playMusic(MusicTrack track, double volume) {
            tracks.add(track);
            musicVolume = volume;
        }

        @Override
        public void setMusicVolume(double volume) {
            musicVolume = volume;
        }

        @Override
        public void stopMusic() {
            stopMusicCalls++;
        }

        @Override
        public void close() {
            closeCalls++;
        }
    }
}
