package com.bomberman.clientfx.ui.component;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import org.kordamp.ikonli.javafx.FontIcon;

/** One line of the ROOM STATUS checklist: a round state icon and a description. */
public final class ChecklistItem extends HBox {

    public enum State {
        DONE("mdi2c-check-bold", "done"),
        NOT_DONE("mdi2c-close-thick", "not-done"),
        WAITING("mdi2d-dots-horizontal", "waiting");

        private final String icon;
        private final String styleClass;

        State(String icon, String styleClass) {
            this.icon = icon;
            this.styleClass = styleClass;
        }
    }

    private final StackPane badge = new StackPane();
    private final FontIcon icon = new FontIcon();
    private final Label text = new Label();

    public ChecklistItem(State state, String description) {
        setSpacing(12);
        setAlignment(Pos.CENTER_LEFT);
        badge.getChildren().add(icon);
        text.getStyleClass().addAll("label-body", "checklist-text");
        getChildren().addAll(badge, text);
        update(state, description);
    }

    public void update(State state, String description) {
        badge.getStyleClass().setAll("check-icon", state.styleClass);
        icon.setIconLiteral(state.icon);
        text.setText(description);
    }
}
