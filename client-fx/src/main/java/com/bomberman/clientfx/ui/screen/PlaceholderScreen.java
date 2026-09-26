package com.bomberman.clientfx.ui.screen;

import com.bomberman.clientfx.ui.Screen;
import com.bomberman.clientfx.ui.ScreenId;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

/** Stands in for screens that later phases will build. */
public final class PlaceholderScreen implements Screen {

    private final StackPane root = new StackPane();

    public PlaceholderScreen(ScreenId id) {
        Label label = new Label(id.name().replace('_', ' ') + " – coming in a later phase");
        label.getStyleClass().add("temp-title");
        root.getChildren().add(label);
    }

    @Override
    public Node root() {
        return root;
    }
}
