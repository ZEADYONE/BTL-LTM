package com.bomberman.clientfx.state;

import com.bomberman.clientfx.network.ClientNetworkConfig;

import java.util.Optional;
import java.util.prefs.Preferences;

/** Per-user settings remembered between launches. Passwords are never stored. */
public final class UserPreferences {

    private static final String NODE = "com/bomberman/clientfx";
    private static final String SERVER_HOST = "serverHost";
    private static final String SERVER_PORT = "serverPort";
    private static final String LAST_USERNAME = "lastUsername";
    private static final String FULLSCREEN = "fullscreen";
    private static final String AUDIO_MASTER_VOLUME = "audioMasterVolume";
    private static final String AUDIO_MUSIC_VOLUME = "audioMusicVolume";
    private static final String AUDIO_SFX_VOLUME = "audioSfxVolume";
    private static final String AUDIO_MUTED = "audioMuted";
    private static final double DEFAULT_MASTER_VOLUME = 0.80;
    private static final double DEFAULT_MUSIC_VOLUME = 0.45;
    private static final double DEFAULT_SFX_VOLUME = 0.80;

    private final Preferences node;

    public UserPreferences(Preferences node) {
        this.node = node;
    }

    public static UserPreferences forCurrentUser() {
        return new UserPreferences(Preferences.userRoot().node(NODE));
    }

    /** The address saved from the Server Address popup, if it is still valid. */
    public Optional<ClientNetworkConfig> savedServer() {
        String host = node.get(SERVER_HOST, null);
        int port = node.getInt(SERVER_PORT, 0);
        try {
            return host == null ? Optional.empty() : Optional.of(new ClientNetworkConfig(host, port));
        } catch (IllegalArgumentException invalidValue) {
            return Optional.empty();
        }
    }

    public void saveServer(ClientNetworkConfig server) {
        node.put(SERVER_HOST, server.host());
        node.putInt(SERVER_PORT, server.port());
    }

    /** Forgets the saved address so the defaults apply again. */
    public void clearServer() {
        node.remove(SERVER_HOST);
        node.remove(SERVER_PORT);
    }

    public String lastUsername() {
        return node.get(LAST_USERNAME, "");
    }

    public void setLastUsername(String username) {
        node.put(LAST_USERNAME, username);
    }

    public boolean fullscreen() {
        return node.getBoolean(FULLSCREEN, false);
    }

    public void setFullscreen(boolean fullscreen) {
        node.putBoolean(FULLSCREEN, fullscreen);
    }

    public double audioMasterVolume() {
        return readVolume(AUDIO_MASTER_VOLUME, DEFAULT_MASTER_VOLUME);
    }

    public void setAudioMasterVolume(double volume) {
        node.putDouble(AUDIO_MASTER_VOLUME, clamp(volume));
    }

    public double audioMusicVolume() {
        return readVolume(AUDIO_MUSIC_VOLUME, DEFAULT_MUSIC_VOLUME);
    }

    public void setAudioMusicVolume(double volume) {
        node.putDouble(AUDIO_MUSIC_VOLUME, clamp(volume));
    }

    public double audioSfxVolume() {
        return readVolume(AUDIO_SFX_VOLUME, DEFAULT_SFX_VOLUME);
    }

    public void setAudioSfxVolume(double volume) {
        node.putDouble(AUDIO_SFX_VOLUME, clamp(volume));
    }

    public boolean audioMuted() {
        return node.getBoolean(AUDIO_MUTED, false);
    }

    public void setAudioMuted(boolean muted) {
        node.putBoolean(AUDIO_MUTED, muted);
    }

    private double readVolume(String key, double fallback) {
        double value = node.getDouble(key, fallback);
        return Double.isFinite(value) && value >= 0 && value <= 1 ? value : fallback;
    }

    private static double clamp(double value) {
        if (!Double.isFinite(value)) {
            return 0;
        }
        return Math.max(0, Math.min(1, value));
    }
}
