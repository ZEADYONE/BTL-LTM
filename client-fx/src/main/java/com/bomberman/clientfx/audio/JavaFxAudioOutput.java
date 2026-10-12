package com.bomberman.clientfx.audio;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.net.URL;
import java.util.EnumMap;
import java.util.Map;

/** JavaFX implementation using cached {@link AudioClip}s and one looping music player. */
public final class JavaFxAudioOutput implements AudioOutput {

    private static final System.Logger LOG = System.getLogger(JavaFxAudioOutput.class.getName());
    private static final Duration MUSIC_FADE = Duration.millis(320);

    private final Map<AudioCue, AudioClip> clips = new EnumMap<>(AudioCue.class);
    private MediaPlayer musicPlayer;
    private MediaPlayer fadingOutPlayer;
    private MusicTrack musicTrack;
    private Timeline musicTransition;
    private boolean closed;

    public JavaFxAudioOutput() {
        for (AudioCue cue : AudioCue.values()) {
            URL resource = JavaFxAudioOutput.class.getResource(cue.resourcePath());
            if (resource == null) {
                LOG.log(System.Logger.Level.WARNING, "Missing audio resource: {0}", cue.resourcePath());
                continue;
            }
            try {
                clips.put(cue, new AudioClip(resource.toExternalForm()));
            } catch (RuntimeException failure) {
                LOG.log(System.Logger.Level.WARNING, "Cannot load audio resource " + cue.resourcePath(), failure);
            }
        }
    }

    @Override
    public void play(AudioCue cue, double volume) {
        if (closed || volume <= 0) {
            return;
        }
        AudioClip clip = clips.get(cue);
        if (clip != null) {
            try {
                clip.play(clamp(volume));
            } catch (RuntimeException failure) {
                LOG.log(System.Logger.Level.WARNING, "Cannot play audio cue " + cue, failure);
            }
        }
    }

    @Override
    public void playMusic(MusicTrack track, double volume) {
        if (closed) {
            return;
        }
        double targetVolume = clamp(volume);
        if (track == musicTrack && musicPlayer != null) {
            setMusicVolume(targetVolume);
            return;
        }

        URL resource = JavaFxAudioOutput.class.getResource(track.resourcePath());
        if (resource == null) {
            LOG.log(System.Logger.Level.WARNING, "Missing music resource: {0}", track.resourcePath());
            return;
        }

        MediaPlayer next;
        try {
            next = new MediaPlayer(new Media(resource.toExternalForm()));
        } catch (RuntimeException failure) {
            LOG.log(System.Logger.Level.WARNING, "Cannot load music resource " + track.resourcePath(), failure);
            return;
        }
        next.setCycleCount(MediaPlayer.INDEFINITE);
        next.setVolume(0);
        next.setOnError(() -> LOG.log(System.Logger.Level.WARNING,
                "Music playback failed for " + track, next.getError()));

        MediaPlayer previous = musicPlayer;
        MusicTrack previousTrack = musicTrack;
        stopTransition();
        musicPlayer = next;
        musicTrack = track;
        try {
            next.play();
            KeyFrame fade = previous == null
                    ? new KeyFrame(MUSIC_FADE, new KeyValue(next.volumeProperty(), targetVolume))
                    : new KeyFrame(
                            MUSIC_FADE,
                            new KeyValue(next.volumeProperty(), targetVolume),
                            new KeyValue(previous.volumeProperty(), 0.0)
                    );
            Timeline transition = new Timeline(fade);
            fadingOutPlayer = previous;
            transition.setOnFinished(event -> {
                if (fadingOutPlayer == previous) {
                    dispose(fadingOutPlayer);
                    fadingOutPlayer = null;
                }
                musicTransition = null;
            });
            musicTransition = transition;
            transition.play();
        } catch (RuntimeException failure) {
            dispose(next);
            musicPlayer = previous;
            musicTrack = previousTrack;
            LOG.log(System.Logger.Level.WARNING, "Cannot play music " + track, failure);
        }
    }

    @Override
    public void setMusicVolume(double volume) {
        if (musicPlayer == null || closed) {
            return;
        }
        stopTransition();
        musicPlayer.setVolume(clamp(volume));
    }

    @Override
    public void stopMusic() {
        if (musicPlayer == null) {
            return;
        }
        stopTransition();
        MediaPlayer previous = musicPlayer;
        musicPlayer = null;
        musicTrack = null;
        Timeline transition = new Timeline(new KeyFrame(
                MUSIC_FADE,
                new KeyValue(previous.volumeProperty(), 0.0)
        ));
        fadingOutPlayer = previous;
        transition.setOnFinished(event -> {
            if (fadingOutPlayer == previous) {
                dispose(fadingOutPlayer);
                fadingOutPlayer = null;
            }
            musicTransition = null;
        });
        musicTransition = transition;
        transition.play();
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        stopTransition();
        dispose(musicPlayer);
        musicPlayer = null;
        musicTrack = null;
        clips.values().forEach(AudioClip::stop);
        clips.clear();
    }

    private void stopTransition() {
        if (musicTransition != null) {
            musicTransition.stop();
            musicTransition = null;
        }
        dispose(fadingOutPlayer);
        fadingOutPlayer = null;
    }

    private static void dispose(MediaPlayer player) {
        if (player != null) {
            player.stop();
            player.dispose();
        }
    }

    private static double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }
}
