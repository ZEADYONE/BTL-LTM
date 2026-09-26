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
    }

    @Test
    void valuesSurviveARoundTrip() {
        preferences.saveServer(new ClientNetworkConfig("192.168.1.20", 9000));
        preferences.setLastUsername("Minh Đức");
        preferences.setFullscreen(true);

        UserPreferences reloaded = new UserPreferences(node);
        assertEquals(Optional.of(new ClientNetworkConfig("192.168.1.20", 9000)), reloaded.savedServer());
        assertEquals("Minh Đức", reloaded.lastUsername());
        assertTrue(reloaded.fullscreen());
    }

    @Test
    void corruptedServerEntryIsIgnored() {
        node.put("serverHost", "lan-server");
        node.putInt("serverPort", 70_000);

        assertEquals(Optional.empty(), preferences.savedServer());
    }
}
