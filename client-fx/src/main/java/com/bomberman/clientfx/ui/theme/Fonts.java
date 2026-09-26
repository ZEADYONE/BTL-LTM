package com.bomberman.clientfx.ui.theme;

import javafx.scene.text.Font;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Registers the bundled fonts with JavaFX so CSS can refer to them by family name.
 * Lilita One has no Vietnamese glyphs, so user-entered text always uses Nunito.
 */
public final class Fonts {

    public static final String DISPLAY = "Lilita One";
    public static final String NAME = "Nunito Black";
    public static final String BODY = "Nunito ExtraBold";

    private static final System.Logger LOG = System.getLogger(Fonts.class.getName());
    private static final List<String> FILES = List.of(
            "LilitaOne-Regular.ttf",
            "Nunito-Black.ttf",
            "Nunito-ExtraBold.ttf",
            "Nunito-Bold.ttf"
    );

    private Fonts() {
    }

    public static void load() {
        for (String file : FILES) {
            try (InputStream input = Fonts.class.getResourceAsStream("/fonts/" + file)) {
                Font font = input == null ? null : Font.loadFont(input, 12);
                if (font == null) {
                    LOG.log(System.Logger.Level.WARNING, "Cannot load font " + file + "; falling back to system fonts");
                }
            } catch (IOException exception) {
                throw new UncheckedIOException("Cannot read font " + file, exception);
            }
        }
    }
}
