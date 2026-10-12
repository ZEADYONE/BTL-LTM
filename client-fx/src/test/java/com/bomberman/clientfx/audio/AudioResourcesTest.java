package com.bomberman.clientfx.audio;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AudioResourcesTest {

    @Test
    void everyCueHasAUniqueWavResource() {
        Set<String> paths = new HashSet<>();
        for (AudioCue cue : AudioCue.values()) {
            assertTrue(paths.add(cue.resourcePath()), "Duplicate path: " + cue.resourcePath());
            assertTrue(cue.resourcePath().endsWith(".wav"), cue + " must use WAV");
            assertNotNull(AudioResourcesTest.class.getResource(cue.resourcePath()), cue.resourcePath());
        }
    }

    @Test
    void everyMusicTrackHasAUniqueMp3Resource() {
        Set<String> paths = new HashSet<>();
        for (MusicTrack track : MusicTrack.values()) {
            assertTrue(paths.add(track.resourcePath()), "Duplicate path: " + track.resourcePath());
            assertTrue(track.resourcePath().endsWith(".mp3"), track + " must use MP3");
            assertNotNull(AudioResourcesTest.class.getResource(track.resourcePath()), track.resourcePath());
        }
    }
}
