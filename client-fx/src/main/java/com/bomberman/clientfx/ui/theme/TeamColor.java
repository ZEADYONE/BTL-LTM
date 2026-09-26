package com.bomberman.clientfx.ui.theme;

import javafx.scene.paint.Color;

import java.util.Locale;

/**
 * The four player colours. RED doubles as the set of key colours drawn in character SVGs,
 * which {@link com.bomberman.clientfx.asset.SvgRecolor} swaps for the other teams.
 */
public enum TeamColor {
    RED("#E53935", "#B71C1C", "#FF8A80"),
    BLUE("#1E88E5", "#0D47A1", "#90CAF9"),
    GREEN("#43A047", "#1B5E20", "#A5D6A7"),
    YELLOW("#FDD835", "#F9A825", "#FFF59D");

    private final String main;
    private final String shade;
    private final String light;

    TeamColor(String main, String shade, String light) {
        this.main = main;
        this.shade = shade;
        this.light = light;
    }

    /** Colour of room slot {@code slotIndex} (0-based), which is also the spawn order. */
    public static TeamColor forSlot(int slotIndex) {
        return values()[Math.floorMod(slotIndex, values().length)];
    }

    /** Representative colour of a player outside a match (decision D7). */
    public static TeamColor forUser(long userId) {
        return values()[(int) Math.floorMod(userId, (long) values().length)];
    }

    public String main() {
        return main;
    }

    public String shade() {
        return shade;
    }

    public String light() {
        return light;
    }

    public Color mainColor() {
        return Color.web(main);
    }

    public Color lightColor() {
        return Color.web(light);
    }

    /** Lower-case name used in PNG fallback file names, e.g. {@code bomber_full_blue.png}. */
    public String fileSuffix() {
        return name().toLowerCase(Locale.ROOT);
    }
}
