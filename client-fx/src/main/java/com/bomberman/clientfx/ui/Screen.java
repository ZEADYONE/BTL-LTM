package com.bomberman.clientfx.ui;

import javafx.scene.Node;

/** One full-frame screen laid out on the 1280×720 design frame. */
public interface Screen {

    Node root();

    /**
     * Whether the screen sits inside the orange frame (1152×648) like the menu mockups.
     * Full-bleed screens such as the match use the whole 1280×720 canvas.
     */
    default boolean framed() {
        return true;
    }

    /** Called after the screen becomes visible; typically subscribes to state and requests data. */
    default void onShow() {
    }

    /** Called before another screen replaces this one. */
    default void onHide() {
    }
}
