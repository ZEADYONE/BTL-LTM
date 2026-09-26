package com.bomberman.clientfx.game;

import com.bomberman.clientfx.ServerRules;
import com.bomberman.clientfx.ServerRules.Cell;
import com.bomberman.common.enums.TileType;

import java.util.List;

/**
 * The server's default arena, rebuilt on the client so the Room Lobby can preview the exact map
 * before the match starts. Mirrors GameMap.createDefault(); update both together.
 */
public final class MapPreview {

    private MapPreview() {
    }

    /** Tiles indexed {@code [row][column]}, like GameStateDto.map(). */
    public static TileType[][] defaultMap() {
        TileType[][] tiles = new TileType[ServerRules.MAP_ROWS][ServerRules.MAP_COLUMNS];
        for (int row = 0; row < ServerRules.MAP_ROWS; row++) {
            for (int column = 0; column < ServerRules.MAP_COLUMNS; column++) {
                if (isBorder(column, row) || (column % 2 == 0 && row % 2 == 0)) {
                    tiles[row][column] = TileType.HARD_WALL;
                } else if ((column * 31 + row * 17) % 4 == 0) {
                    tiles[row][column] = TileType.BREAKABLE_WALL;
                } else {
                    tiles[row][column] = TileType.EMPTY;
                }
            }
        }
        for (Cell spawn : ServerRules.SPAWNS) {
            tiles[spawn.row()][spawn.column()] = TileType.EMPTY;
            for (Cell neighbour : List.of(
                    new Cell(spawn.column(), spawn.row() - 1),
                    new Cell(spawn.column(), spawn.row() + 1),
                    new Cell(spawn.column() - 1, spawn.row()),
                    new Cell(spawn.column() + 1, spawn.row()))) {
                if (!isBorder(neighbour.column(), neighbour.row())) {
                    tiles[neighbour.row()][neighbour.column()] = TileType.EMPTY;
                }
            }
        }
        return tiles;
    }

    private static boolean isBorder(int column, int row) {
        return column == 0 || column == ServerRules.MAP_COLUMNS - 1 || row == 0 || row == ServerRules.MAP_ROWS - 1;
    }
}
