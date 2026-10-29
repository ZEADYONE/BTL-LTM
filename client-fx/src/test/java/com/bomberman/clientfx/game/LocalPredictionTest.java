package com.bomberman.clientfx.game;

import com.bomberman.common.dto.BombStateDto;
import com.bomberman.common.dto.GamePlayerStateDto;
import com.bomberman.common.dto.GameStateDto;
import com.bomberman.common.dto.PositionDto;
import com.bomberman.common.enums.Direction;
import com.bomberman.common.enums.GameStatus;
import com.bomberman.common.enums.TileType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LocalPredictionTest {

    private static final long ME = 1;
    private static final long OTHER = 2;

    @Test
    void stepIsShownBeforeTheServerConfirmsIt() {
        LocalPrediction prediction = new LocalPrediction();
        prediction.accept(snapshot(me(2, 2, true, 0), other(8, 8)), ME);

        prediction.predictMove(Direction.RIGHT);
        prediction.predictMove(Direction.RIGHT);

        assertEquals(new PositionDto(4, 2), prediction.position());
        assertEquals(2, prediction.pendingCount());
    }

    @Test
    void acknowledgedStepsAreDroppedAndTheRestReplayed() {
        LocalPrediction prediction = new LocalPrediction();
        prediction.accept(snapshot(me(2, 2, true, 0), other(8, 8)), ME);
        long first = prediction.predictMove(Direction.RIGHT);
        prediction.predictMove(Direction.DOWN);

        prediction.accept(snapshot(me(3, 2, true, first), other(8, 8)), ME);

        assertEquals(new PositionDto(3, 3), prediction.position());
        assertEquals(1, prediction.pendingCount());
    }

    @Test
    void serverRejectionSnapsBackToTheAuthoritativeTile() {
        LocalPrediction prediction = new LocalPrediction();
        prediction.accept(snapshot(me(2, 2, true, 0), other(8, 8)), ME);
        long seq = prediction.predictMove(Direction.RIGHT);

        // Another player stepped onto (3, 2) first, so the server refused the step.
        prediction.accept(snapshot(me(2, 2, true, seq), other(3, 2)), ME);

        assertEquals(new PositionDto(2, 2), prediction.position());
        assertEquals(0, prediction.pendingCount());
    }

    @Test
    void wallsBombsAndPlayersBlockThePrediction() {
        LocalPrediction prediction = new LocalPrediction();
        List<List<TileType>> map = emptyMap();
        map.get(2).set(3, TileType.BREAKABLE_WALL);
        BombStateDto bomb = new BombStateDto("b", OTHER, new PositionDto(2, 3), 2, 3_000);
        prediction.accept(new GameStateDto(5, GameStatus.RUNNING, map,
                List.of(me(2, 2, true, 0), other(1, 2)), List.of(bomb), List.of(), 2), ME);

        prediction.predictMove(Direction.RIGHT);
        prediction.predictMove(Direction.DOWN);
        prediction.predictMove(Direction.LEFT);

        assertEquals(new PositionDto(2, 2), prediction.position());
        assertEquals(3, prediction.pendingCount());
    }

    @Test
    void deadPlayerIsNotPredicted() {
        LocalPrediction prediction = new LocalPrediction();
        prediction.accept(snapshot(me(2, 2, false, 0), other(8, 8)), ME);

        prediction.predictMove(Direction.RIGHT);

        assertEquals(new PositionDto(2, 2), prediction.position());
        assertEquals(0, prediction.pendingCount());
    }

    @Test
    void seqKeepsIncreasingAcrossMatches() {
        LocalPrediction prediction = new LocalPrediction();
        long first = prediction.predictMove(Direction.UP);
        prediction.reset();

        assertEquals(first + 1, prediction.predictMove(Direction.UP));
        assertNull(prediction.position());
    }

    private static GameStateDto snapshot(GamePlayerStateDto... players) {
        return new GameStateDto(5, GameStatus.RUNNING, emptyMap(), List.of(players), List.of(), List.of(),
                players.length);
    }

    private static List<List<TileType>> emptyMap() {
        List<List<TileType>> rows = new ArrayList<>();
        for (int row = 0; row < 11; row++) {
            rows.add(new ArrayList<>(Collections.nCopies(13, TileType.EMPTY)));
        }
        return rows;
    }

    private static GamePlayerStateDto me(int column, int row, boolean alive, long lastMoveSeq) {
        return new GamePlayerStateDto(ME, "me", new PositionDto(column, row), alive, 1, 0, 2, lastMoveSeq);
    }

    private static GamePlayerStateDto other(int column, int row) {
        return new GamePlayerStateDto(OTHER, "other", new PositionDto(column, row), true, 1, 0, 2, 0);
    }
}
