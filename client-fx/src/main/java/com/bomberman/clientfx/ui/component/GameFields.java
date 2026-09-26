package com.bomberman.clientfx.ui.component;

import javafx.css.PseudoClass;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;

/** Factory and helpers for the cream, thick-bordered text inputs. */
public final class GameFields {

    private static final PseudoClass ERROR = PseudoClass.getPseudoClass("error");

    private GameFields() {
    }

    public static TextField text(String prompt) {
        TextField field = new TextField();
        style(field, prompt);
        return field;
    }

    public static PasswordField password(String prompt) {
        PasswordField field = new PasswordField();
        style(field, prompt);
        return field;
    }

    /** Red border while {@code error} is true; cleared again as soon as the player types. */
    public static void markError(TextInputControl field, boolean error) {
        field.pseudoClassStateChanged(ERROR, error);
    }

    private static void style(TextInputControl field, String prompt) {
        field.getStyleClass().add("game-field");
        field.setPromptText(prompt);
        field.textProperty().addListener(observable -> markError(field, false));
    }
}
