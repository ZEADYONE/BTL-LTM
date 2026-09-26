package com.bomberman.clientfx.ui;

import javafx.scene.Node;

/** One full-frame screen laid out on the 1280×720 design frame. */
public interface Screen {

    Node root();

    /** Called after the screen becomes visible; typically subscribes to state and requests data. */
    default void onShow() {
    }

    /** Called before another screen replaces this one. */
    default void onHide() {
    }
}
