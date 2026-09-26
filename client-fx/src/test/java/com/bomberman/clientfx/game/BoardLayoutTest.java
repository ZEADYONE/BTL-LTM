package com.bomberman.clientfx.game;

import com.bomberman.clientfx.ServerRules;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoardLayoutTest {

    @Test
    void fitsAndCentresTheArenaBelowTheHud() {
        BoardLayout layout = BoardLayout.fit(1280, 720);

        assertEquals(52, layout.tile());
        assertEquals((1280 - 52 * ServerRules.MAP_COLUMNS) / 2.0, layout.left());
        assertTrue(layout.top() >= BoardLayout.HUD_HEIGHT + BoardLayout.MARGIN);
        assertTrue(layout.top() + layout.boardHeight() <= 720 - BoardLayout.MARGIN);
    }

    @Test
    void floorStartsOneQuarterTileBelowBlockArtwork() {
        BoardLayout layout = BoardLayout.fit(1280, 720);

        assertEquals(layout.tile() * 0.25, layout.cellY(0) - layout.blockY(0));
        assertEquals(layout.left() + layout.tile() * 4, layout.cellX(4));
    }
}
