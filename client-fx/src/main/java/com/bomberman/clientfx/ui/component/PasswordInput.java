package com.bomberman.clientfx.ui.component;

import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Password box with an eye button that reveals what was typed. A masked and a plain field
 * share the same text; the button swaps which one is visible and keeps the caret in place.
 */
public final class PasswordInput extends StackPane {

    private static final String SHOW_ICON = "mdi2e-eye-outline";
    private static final String HIDE_ICON = "mdi2e-eye-off-outline";

    private final PasswordField masked;
    private final TextField plain;
    private final FontIcon icon = new FontIcon(SHOW_ICON);
    private final Tooltip tooltip = new Tooltip("Show password");
    private boolean revealed;

    public PasswordInput(String prompt) {
        masked = GameFields.password(prompt);
        plain = GameFields.text(prompt);
        plain.textProperty().bindBidirectional(masked.textProperty());
        masked.getStyleClass().add("with-reveal");
        plain.getStyleClass().add("with-reveal");
        plain.setVisible(false);

        Button toggle = new Button();
        toggle.getStyleClass().setAll("reveal-toggle");
        toggle.setGraphic(icon);
        toggle.setTooltip(tooltip);
        toggle.setFocusTraversable(false);
        toggle.setOnAction(event -> setRevealed(!revealed));
        StackPane.setAlignment(toggle, Pos.CENTER_RIGHT);
        StackPane.setMargin(toggle, new Insets(0, 8, 0, 0));

        getChildren().addAll(masked, plain, toggle);
    }

    public void setRevealed(boolean revealed) {
        TextInputControl from = visibleField();
        int caret = from.getCaretPosition();
        boolean hadFocus = from.isFocused();
        this.revealed = revealed;
        masked.setVisible(!revealed);
        plain.setVisible(revealed);
        icon.setIconLiteral(revealed ? HIDE_ICON : SHOW_ICON);
        tooltip.setText(revealed ? "Hide password" : "Show password");
        TextInputControl to = visibleField();
        if (hadFocus || from == to) {
            to.requestFocus();
        }
        to.positionCaret(caret);
    }

    public boolean isRevealed() {
        return revealed;
    }

    public String getText() {
        return masked.getText();
    }

    public void clear() {
        masked.clear();
    }

    public StringProperty textProperty() {
        return masked.textProperty();
    }

    public void markError(boolean error) {
        GameFields.markError(masked, error);
        GameFields.markError(plain, error);
    }

    public void focusField() {
        visibleField().requestFocus();
    }

    private TextInputControl visibleField() {
        return revealed ? plain : masked;
    }
}
