package com.bomberman.clientfx.state;

import com.bomberman.clientfx.network.ClientNetworkConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserPreferencesTest {

    private final Preferences node = Preferences.userRoot().node("com/bomberman/clientfx-test-" + UUID.randomUUID());
    private final UserPreferences preferences = new UserPreferences(node);

    @AfterEach
    void removeNode() throws BackingStoreException {
        node.removeNode();
    }

    @Test
    void emptyNodeHasDefaults() {
        assertEquals(Optional.empty(), preferences.savedServer());
        assertEquals("", preferences.lastUsername());
        assertFalse(preferences.fullscreen());
        assertEquals(0.80, preferences.audioMasterVolume());
        assertEquals(0.45, preferences.audioMusicVolume());
        assertEquals(0.80, preferences.audioSfxVolume());
        assertFalse(preferences.audioMuted());
    }

    @Test
    void valuesSurviveARoundTrip() {
        preferences.saveServer(new ClientNetworkConfig("192.168.1.20", 9000));
        preferences.setLastUsername("Minh Đức");
        preferences.setFullscreen(true);
        preferences.setAudioMasterVolume(0.65);
        preferences.setAudioMusicVolume(0.35);
        preferences.setAudioSfxVolume(0.90);
        preferences.setAudioMuted(true);

        UserPreferences reloaded = new UserPreferences(node);
        assertEquals(Optional.of(new ClientNetworkConfig("192.168.1.20", 9000)), reloaded.savedServer());
        assertEquals("Minh Đức", reloaded.lastUsername());
        assertTrue(reloaded.fullscreen());
        assertEquals(0.65, reloaded.audioMasterVolume());
        assertEquals(0.35, reloaded.audioMusicVolume());
        assertEquals(0.90, reloaded.audioSfxVolume());
        assertTrue(reloaded.audioMuted());
    }

    @Test
    void clearedServerFallsBackToDefaults() {
        preferences.saveServer(new ClientNetworkConfig("192.168.1.20", 9000));

        preferences.clearServer();

        assertEquals(Optional.empty(), preferences.savedServer());
    }

    @Test
    void corruptedServerEntryIsIgnored() {
        node.put("serverHost", "lan-server");
        node.putInt("serverPort", 70_000);

        assertEquals(Optional.empty(), preferences.savedServer());
    }

    @Test
    void invalidAudioValuesFallBackAndSettersClamp() {
        node.putDouble("audioMasterVolume", Double.NaN);
        node.putDouble("audioMusicVolume", 1.5);
        node.putDouble("audioSfxVolume", -0.2);

        assertEquals(0.80, preferences.audioMasterVolume());
        assertEquals(0.45, preferences.audioMusicVolume());
        assertEquals(0.80, preferences.audioSfxVolume());

        preferences.setAudioMasterVolume(2.0);
        preferences.setAudioMusicVolume(-1.0);
        preferences.setAudioSfxVolume(Double.NaN);

        assertEquals(1.0, preferences.audioMasterVolume());
        assertEquals(0.0, preferences.audioMusicVolume());
        assertEquals(0.0, preferences.audioSfxVolume());
    }
}
