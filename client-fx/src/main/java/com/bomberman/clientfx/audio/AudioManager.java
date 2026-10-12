package com.bomberman.clientfx.audio;

import com.bomberman.clientfx.state.Feedback;
import com.bomberman.clientfx.state.UserPreferences;
import com.bomberman.clientfx.ui.ScreenId;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.LongSupplier;

/** Owns audio policy, persisted volume settings and per-cue cooldowns. */
public final class AudioManager implements AutoCloseable {

    private final AudioOutput output;
    private final UserPreferences preferences;
    private final LongSupplier nanoTime;
    private final Map<AudioCue, Long> lastPlayedNanos = new EnumMap<>(AudioCue.class);

    private double masterVolume;
    private double musicVolume;
    private double sfxVolume;
    private boolean muted;
    private boolean closed;
    private MusicTrack currentMusicTrack;

    public AudioManager(AudioOutput output, UserPreferences preferences) {
        this(output, preferences, System::nanoTime);
    }

    AudioManager(AudioOutput output, UserPreferences preferences, LongSupplier nanoTime) {
        this.output = Objects.requireNonNull(output, "output must not be null");
        this.preferences = Objects.requireNonNull(preferences, "preferences must not be null");
        this.nanoTime = Objects.requireNonNull(nanoTime, "nanoTime must not be null");
        masterVolume = preferences.audioMasterVolume();
        musicVolume = preferences.audioMusicVolume();
        sfxVolume = preferences.audioSfxVolume();
        muted = preferences.audioMuted();
    }

    public void play(AudioCue cue) {
        Objects.requireNonNull(cue, "cue must not be null");
        double volume = effectiveSfxVolume() * cue.gain();
        if (closed || volume <= 0) {
            return;
        }
        long now = nanoTime.getAsLong();
        Long lastPlayed = lastPlayedNanos.get(cue);
        if (lastPlayed != null && now - lastPlayed < cue.cooldown().toNanos()) {
            return;
        }
        lastPlayedNanos.put(cue, now);
        output.play(cue, volume);
    }

    public void playFeedback(Feedback feedback) {
        switch (feedback.kind()) {
            case SUCCESS -> play(AudioCue.UI_SUCCESS);
            case ERROR -> play(AudioCue.UI_ERROR);
            case INFO -> {
                // Informational toasts are frequent and intentionally silent.
            }
        }
    }

    public void onScreenChanged(ScreenId screen) {
        if (closed) {
            return;
        }
        MusicTrack target = switch (screen) {
            case GAME -> MusicTrack.BATTLE;
            case RESULT -> null;
            default -> MusicTrack.MENU;
        };
        if (target == currentMusicTrack) {
            return;
        }
        currentMusicTrack = target;
        if (target == null) {
            output.stopMusic();
        } else {
            output.playMusic(target, effectiveMusicVolume());
        }
    }

    public double masterVolume() {
        return masterVolume;
    }

    public double musicVolume() {
        return musicVolume;
    }

    public double sfxVolume() {
        return sfxVolume;
    }

    public boolean muted() {
        return muted;
    }

    public void setMasterVolume(double value) {
        masterVolume = clamp(value);
        preferences.setAudioMasterVolume(masterVolume);
        refreshMusicVolume();
    }

    public void setMusicVolume(double value) {
        musicVolume = clamp(value);
        preferences.setAudioMusicVolume(musicVolume);
        refreshMusicVolume();
    }

    public void setSfxVolume(double value) {
        sfxVolume = clamp(value);
        preferences.setAudioSfxVolume(sfxVolume);
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
        preferences.setAudioMuted(muted);
        refreshMusicVolume();
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        currentMusicTrack = null;
        output.close();
        lastPlayedNanos.clear();
    }

    private double effectiveMusicVolume() {
        return muted ? 0 : masterVolume * musicVolume;
    }

    private double effectiveSfxVolume() {
        return muted ? 0 : masterVolume * sfxVolume;
    }

    private void refreshMusicVolume() {
        if (!closed) {
            output.setMusicVolume(effectiveMusicVolume());
        }
    }

    private static double clamp(double value) {
        if (!Double.isFinite(value)) {
            return 0;
        }
        return Math.max(0, Math.min(1, value));
    }
}
