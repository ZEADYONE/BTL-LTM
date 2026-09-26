package com.bomberman.clientfx.game;

import com.bomberman.clientfx.ServerRules;
import com.bomberman.clientfx.ServerRules.Cell;
import com.bomberman.common.enums.TileType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The preview must match GameMap.createDefault() on the server (docs/ui-redesign/03, section 9). */
class MapPreviewTest {

    private final TileType[][] map = MapPreview.defaultMap();

    @Test
    void hasTheServerDimensions() {
        assertEquals(ServerRules.MAP_ROWS, map.length);
        Arrays.stream(map).forEach(row -> assertEquals(ServerRules.MAP_COLUMNS, row.length));
    }

    @Test
    void borderAndEvenEvenCellsAreStone() {
        for (int column = 0; column < ServerRules.MAP_COLUMNS; column++) {
            assertEquals(TileType.HARD_WALL, map[0][column]);
            assertEquals(TileType.HARD_WALL, map[ServerRules.MAP_ROWS - 1][column]);
        }
        assertEquals(TileType.HARD_WALL, map[5][0]);
        assertEquals(TileType.HARD_WALL, map[2][2]);
        assertEquals(TileType.HARD_WALL, map[8][10]);
    }

    @Test
    void cratesFollowTheServerFormula() {
        // (column * 31 + row * 17) % 4 == 0
        assertEquals(TileType.BREAKABLE_WALL, map[3][3]);
        assertEquals(TileType.BREAKABLE_WALL, map[1][5]);
        assertEquals(TileType.EMPTY, map[1][3]);
    }

    @Test
    void spawnsAndTheirNeighboursAreClear() {
        for (Cell spawn : ServerRules.SPAWNS) {
            assertEquals(TileType.EMPTY, map[spawn.row()][spawn.column()], spawn.toString());
            assertEquals(TileType.EMPTY, map[spawn.row()][spawn.column() + (spawn.column() == 1 ? 1 : -1)]);
            assertEquals(TileType.EMPTY, map[spawn.row() + (spawn.row() == 1 ? 1 : -1)][spawn.column()]);
        }
    }
}
