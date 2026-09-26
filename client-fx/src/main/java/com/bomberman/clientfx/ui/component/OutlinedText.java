package com.bomberman.clientfx.ui.component;

import com.bomberman.clientfx.ui.theme.Fonts;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.Stop;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

/**
 * Chunky Lilita One text with a dark outline and a hard drop shadow (titles, button labels).
 *
 * <p>The outline is a second copy of the text drawn behind with a centred stroke twice as wide.
 * JavaFX renders {@link StrokeType#OUTSIDE} with area geometry on the CPU for every repaint,
 * which saturated the render thread; a centred stroke uses the fast path and looks the same.</p>
 */
public final class OutlinedText extends StackPane {

    public enum Style {
        DISPLAY(96, 7, 5),
        TITLE(56, 5, 4),
        HEADING(32, 3.5, 3),
        BUTTON_XL(60, 3.5, 4),
        BUTTON_L(28, 2.2, 2),
        BUTTON_M(22, 2, 2),
        MENU(24, 2, 2),
        LABEL(18, 1.6, 1.5);

        private final double size;
        private final double outline;
        private final double shadow;

        Style(double size, double outline, double shadow) {
            this.size = size;
            this.outline = outline;
            this.shadow = shadow;
        }
    }

    /** Gold fill of the VICTORY title. */
    public static final Paint GOLD = verticalGradient("#FFF3B0", "#FFC93C", "#F29F05");
    /** Blue-grey fill of the DEFEAT title. */
    public static final Paint STEEL = verticalGradient("#C3CEF0", "#7A8BB8", "#4E5D8C");
    /** Silver fill of the DRAW title. */
    public static final Paint SILVER = verticalGradient("#FFFFFF", "#D9DCE6", "#A3A8BA");

    private static final Color OUTLINE = Color.web("#2A1F3D");

    private final Text outline = new Text();
    private final Text face = new Text();

    public OutlinedText(String text, Style style) {
        this(text, style, Color.WHITE);
    }

    public OutlinedText(String text, Style style, Paint fill) {
        Font font = Font.font(Fonts.DISPLAY, style.size);
        outline.setFont(font);
        outline.setFill(OUTLINE);
        outline.setStroke(OUTLINE);
        outline.setStrokeType(StrokeType.CENTERED);
        outline.setStrokeWidth(style.outline * 2);
        outline.setStrokeLineJoin(StrokeLineJoin.ROUND);
        face.setFont(font);
        face.setFill(fill);
        setText(text);
        getStyleClass().add("outlined-text");
        getChildren().addAll(outline, face);
        setEffect(new DropShadow(BlurType.ONE_PASS_BOX, Color.rgb(42, 31, 61, 0.6), 0, 0, 0, style.shadow));
        // Labels rarely change, so keep the rendered text and shadow as a texture instead of
        // redrawing both on every repaint of the area around them.
        setCache(true);
        setMouseTransparent(true);
        setMinSize(USE_PREF_SIZE, USE_PREF_SIZE);
    }

    public void setText(String text) {
        outline.setText(text);
        face.setText(text);
    }

    public String getText() {
        return face.getText();
    }

    private static Paint verticalGradient(String top, String middle, String bottom) {
        return new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web(top)), new Stop(0.45, Color.web(middle)), new Stop(1, Color.web(bottom)));
    }
}
