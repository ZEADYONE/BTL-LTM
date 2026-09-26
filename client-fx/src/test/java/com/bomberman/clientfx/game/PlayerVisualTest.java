package com.bomberman.clientfx.game;

import com.bomberman.common.dto.PositionDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerVisualTest {

    @Test
    void reachesOneTileTargetAtTheConfiguredSpeed() {
        PlayerVisual visual = new PlayerVisual(new PositionDto(1, 1));
        visual.setTarget(new PositionDto(2, 1));

        visual.advance(0.130);

        assertEquals(2, visual.x(), 0.001);
        assertEquals(PlayerVisual.Facing.RIGHT, visual.facing());
        assertFalse(visual.moving());
    }

    @Test
    void doublesSpeedWhenMoreThanOneTileBehind() {
        PlayerVisual visual = new PlayerVisual(new PositionDto(1, 1));
        visual.setTarget(new PositionDto(3, 1));

        visual.advance(0.065);

        assertEquals(2, visual.x(), 0.001);
        assertTrue(visual.moving());
    }

    @Test
    void snapsWhenMoreThanTwoAndAHalfTilesBehind() {
        PlayerVisual visual = new PlayerVisual(new PositionDto(1, 1));
        visual.setTarget(new PositionDto(5, 1));

        visual.advance(0.001);

        assertEquals(5, visual.x(), 0.001);
        assertFalse(visual.moving());
    }

    @Test
    void standingStillKeepsThePreviousFacing() {
        PlayerVisual visual = new PlayerVisual(new PositionDto(2, 2));
        visual.setTarget(new PositionDto(2, 1));
        visual.advance(0.2);
        visual.setTarget(new PositionDto(2, 1));

        assertEquals(PlayerVisual.Facing.UP, visual.facing());
    }
}
