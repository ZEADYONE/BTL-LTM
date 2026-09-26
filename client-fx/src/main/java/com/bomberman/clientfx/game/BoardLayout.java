package com.bomberman.clientfx.game;

import com.bomberman.clientfx.ServerRules;

/** Computes the logical-pixel rectangle occupied by the 13x11 arena. */
public record BoardLayout(double canvasWidth, double canvasHeight, double tile, double left, double top) {

    public static final double HUD_HEIGHT = 96;
    public static final double MARGIN = 16;
    public static final double BLOCK_OVERHANG_TILES = 0.25;

    public static BoardLayout fit(double width, double height) {
        if (width <= 0 || height <= HUD_HEIGHT + MARGIN * 2) {
            throw new IllegalArgumentException("Canvas is too small for the game board");
        }
        double availableWidth = width - MARGIN * 2;
        double availableHeight = height - HUD_HEIGHT - MARGIN * 2;
        double tile = Math.floor(Math.min(
                availableWidth / ServerRules.MAP_COLUMNS,
                availableHeight / (ServerRules.MAP_ROWS + BLOCK_OVERHANG_TILES)
        ));
        tile = Math.max(1, tile);
        double boardWidth = tile * ServerRules.MAP_COLUMNS;
        double boardHeight = tile * (ServerRules.MAP_ROWS + BLOCK_OVERHANG_TILES);
        double left = Math.floor((width - boardWidth) / 2);
        double top = HUD_HEIGHT + MARGIN + Math.floor((availableHeight - boardHeight) / 2);
        return new BoardLayout(width, height, tile, left, top);
    }

    /** Top-left x of a map cell. */
    public double cellX(double column) {
        return left + column * tile;
    }

    /** Top-left y of a floor cell; blocks extend another quarter tile above it. */
    public double cellY(double row) {
        return top + BLOCK_OVERHANG_TILES * tile + row * tile;
    }

    public double blockY(double row) {
        return top + row * tile;
    }

    public double boardWidth() {
        return tile * ServerRules.MAP_COLUMNS;
    }

    public double boardHeight() {
        return tile * (ServerRules.MAP_ROWS + BLOCK_OVERHANG_TILES);
    }
}
