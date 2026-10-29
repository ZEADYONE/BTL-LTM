package com.bomberman.clientfx.ui;

/** Switches the visible screen. Must be called on the JavaFX application thread. */
public interface Navigator {

    void show(ScreenId screen);

    /** The visible screen, or {@code null} before the first {@link #show}. */
    ScreenId current();
}
