package com.bomberman.clientfx.ui.component;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

/** Dark rounded counter with an icon, e.g. total score or players online (mockup 1, top right). */
public final class Pill extends HBox {

    private final Label value = new Label();

    public Pill(Node icon, String initialValue) {
        getStyleClass().add("pill");
        value.getStyleClass().add("pill-value");
        value.setText(initialValue);
        getChildren().addAll(icon, value);
        setMaxWidth(USE_PREF_SIZE);
    }

    public void setValue(String text) {
        value.setText(text);
    }
}
