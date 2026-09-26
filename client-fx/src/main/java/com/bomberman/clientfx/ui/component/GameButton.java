package com.bomberman.clientfx.ui.component;

import javafx.animation.ScaleTransition;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

import java.util.Locale;

/**
 * Chunky raised button from the mockups: dark bottom edge, top shine, outlined label.
 * The label is an {@link OutlinedText} graphic; the plain text is kept for accessibility.
 */
public class GameButton extends Button {

    public enum Tone {
        YELLOW,
        BLUE,
        GREEN,
        RED,
        PURPLE,
        GREY
    }

    public enum Size {
        XL(OutlinedText.Style.BUTTON_XL, 18),
        L(OutlinedText.Style.BUTTON_L, 12),
        M(OutlinedText.Style.BUTTON_M, 10),
        S(OutlinedText.Style.LABEL, 8),
        ICON(OutlinedText.Style.BUTTON_M, 0);

        private final OutlinedText.Style textStyle;
        private final double iconGap;

        Size(OutlinedText.Style textStyle, double iconGap) {
            this.textStyle = textStyle;
            this.iconGap = iconGap;
        }
    }

    private static final Duration HOVER = Duration.millis(120);

    private final ScaleTransition hoverScale = new ScaleTransition(HOVER, this);
    private final HBox content = new HBox();
    private final OutlinedText label;
    private Node icon;
    private boolean loading;

    public GameButton(String text, Tone tone, Size size) {
        this(text, tone, size, size.textStyle);
    }

    public GameButton(String text, Tone tone, Size size, Node icon) {
        this(text, tone, size);
        setIcon(icon);
    }

    /** For variants whose label size differs from the button size, e.g. the Home menu tiles. */
    protected GameButton(String text, Tone tone, Size size, OutlinedText.Style textStyle) {
        super(text);
        getStyleClass().setAll("game-button", css("btn-", tone.name()), css("btn-", size.name()));
        setMnemonicParsing(false);
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        label = new OutlinedText(text, textStyle);
        content.setAlignment(Pos.CENTER);
        content.setSpacing(size.iconGap);
        content.setMouseTransparent(true);
        setGraphic(content);
        refreshContent();
        hoverProperty().addListener((observable, wasHovered, hovered) -> animateHover(hovered));
    }

    /** Places {@code icon} before the label; {@code null} removes it. */
    public final void setIcon(Node icon) {
        this.icon = icon;
        if (!loading) {
            refreshContent();
        }
    }

    /** Shows a spinner in place of the icon and blocks clicks while a request is in flight. */
    public void setLoading(boolean loading) {
        if (this.loading == loading) {
            return;
        }
        this.loading = loading;
        refreshContent();
        setDisable(loading);
    }

    public boolean isLoading() {
        return loading;
    }

    private void refreshContent() {
        content.getChildren().clear();
        Node leading = loading ? new ProgressIndicator() : icon;
        if (leading != null) {
            content.getChildren().add(leading);
        }
        if (!getText().isEmpty()) {
            content.getChildren().add(label);
        }
    }

    private void animateHover(boolean hovered) {
        double target = hovered && !isDisabled() ? 1.03 : 1.0;
        hoverScale.stop();
        hoverScale.setToX(target);
        hoverScale.setToY(target);
        hoverScale.playFromStart();
    }

    private static String css(String prefix, String name) {
        return prefix + name.toLowerCase(Locale.ROOT);
    }
}
