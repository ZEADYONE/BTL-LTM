package com.bomberman.clientfx.ui.component;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.Locale;

/** Outlined panel with an optional dark header strip (mockup 3 description box, mockup 4 panels). */
public final class Panel extends VBox {

    public enum Style {
        CREAM,
        PURPLE,
        BROWN
    }

    private final VBox body = new VBox(10);

    public Panel(Style style, String title, Node... content) {
        getStyleClass().addAll("panel", "panel-" + style.name().toLowerCase(Locale.ROOT));
        if (title != null) {
            Label header = new Label(title);
            header.getStyleClass().addAll("panel-header", "label-display");
            header.setStyle("-fx-font-size: 22px;");
            header.setMaxWidth(Double.MAX_VALUE);
            getChildren().add(header);
        }
        body.getStyleClass().add("panel-body");
        body.getChildren().addAll(content);
        getChildren().add(body);
    }

    public VBox body() {
        return body;
    }
}
