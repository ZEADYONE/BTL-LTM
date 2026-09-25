package com.bomberman.server.game;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameMapTest {

    @Test
    void defaultMapHasRequiredDimensionsAndNoNullTiles() {
        GameMap map = GameMap.createDefault();

        for (int row = 0; row < GameMap.ROWS; row++) {
            for (int column = 0; column < GameMap.COLUMNS; column++) {
                assertTrue(map.isInside(new Position(column, row)));
                map.getTile(new Position(column, row));
            }
        }
        assertFalse(map.isInside(new Position(-1, 0)));
        assertFalse(map.isInside(new Position(GameMap.COLUMNS, 0)));
        assertFalse(map.isInside(new Position(0, GameMap.ROWS)));
    }

    @Test
    void borderAndEvenGridPillarsAreHardWalls() {
        GameMap map = GameMap.createDefault();

        for (int column = 0; column < GameMap.COLUMNS; column++) {
            assertEquals(Tile.HARD_WALL, map.getTile(new Position(column, 0)));
            assertEquals(Tile.HARD_WALL, map.getTile(new Position(column, GameMap.ROWS - 1)));
        }
        for (int row = 0; row < GameMap.ROWS; row++) {
            assertEquals(Tile.HARD_WALL, map.getTile(new Position(0, row)));
            assertEquals(Tile.HARD_WALL, map.getTile(new Position(GameMap.COLUMNS - 1, row)));
        }
        for (int row = 2; row < GameMap.ROWS - 1; row += 2) {
            for (int column = 2; column < GameMap.COLUMNS - 1; column += 2) {
                assertEquals(Tile.HARD_WALL, map.getTile(new Position(column, row)));
            }
        }
    }

    @Test
    void hasFourFixedCornerSpawnPositions() {
        assertEquals(
                List.of(
                        new Position(1, 1),
                        new Position(11, 1),
                        new Position(1, 9),
                        new Position(11, 9)
                ),
                GameMap.createDefault().getSpawnPositions()
        );
    }

    @Test
    void spawnAndInteriorAdjacentCellsAreEmpty() {
        GameMap map = GameMap.createDefault();

        for (Position spawn : map.getSpawnPositions()) {
            assertEquals(Tile.EMPTY, map.getTile(spawn));
            for (Position neighbor : List.of(
                    spawn.move(com.bomberman.common.enums.Direction.UP),
                    spawn.move(com.bomberman.common.enums.Direction.DOWN),
                    spawn.move(com.bomberman.common.enums.Direction.LEFT),
                    spawn.move(com.bomberman.common.enums.Direction.RIGHT)
            )) {
                if (neighbor.column() > 0
                        && neighbor.column() < GameMap.COLUMNS - 1
                        && neighbor.row() > 0
                        && neighbor.row() < GameMap.ROWS - 1) {
                    assertEquals(Tile.EMPTY, map.getTile(neighbor));
                }
            }
        }
    }

    @Test
    void defaultMapContainsBreakableWalls() {
        GameMap map = GameMap.createDefault();
        boolean hasBreakableWall = false;

        for (int row = 0; row < GameMap.ROWS; row++) {
            for (int column = 0; column < GameMap.COLUMNS; column++) {
                hasBreakableWall |= map.getTile(new Position(column, row)) == Tile.BREAKABLE_WALL;
            }
        }

        assertTrue(hasBreakableWall);
    }

    @Test
    void customMapIsDefensivelyCopiedAndMustHaveExactDimensions() {
        Tile[][] source = emptyTiles();
        GameMap map = GameMap.fromTiles(source);
        source[1][1] = Tile.HARD_WALL;

        assertEquals(Tile.EMPTY, map.getTile(new Position(1, 1)));
        assertThrows(
                IllegalArgumentException.class,
                () -> GameMap.fromTiles(new Tile[GameMap.ROWS - 1][GameMap.COLUMNS])
        );
    }

    private Tile[][] emptyTiles() {
        Tile[][] tiles = new Tile[GameMap.ROWS][GameMap.COLUMNS];
        for (int row = 0; row < GameMap.ROWS; row++) {
            java.util.Arrays.fill(tiles[row], Tile.EMPTY);
        }
        return tiles;
    }
}
