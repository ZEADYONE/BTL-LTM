package com.bomberman.clientfx.audio;

/** Output boundary kept small so audio policy can be unit-tested without JavaFX media or speakers. */
public interface AudioOutput extends AutoCloseable {

    void play(AudioCue cue, double volume);

    void playMusic(MusicTrack track, double volume);

    void setMusicVolume(double volume);

    void stopMusic();

    @Override
    void close();
}
