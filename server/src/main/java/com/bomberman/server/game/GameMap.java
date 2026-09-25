package com.bomberman.server.game;

import java.util.List;
import java.util.Objects;

/** Authoritative 13-by-11 Bomberman tile map. */
public final class GameMap {

    public static final int COLUMNS = 13;
    public static final int ROWS = 11;

    private static final List<Position> SPAWN_POSITIONS = List.of(
            new Position(1, 1),
            new Position(COLUMNS - 2, 1),
            new Position(1, ROWS - 2),
            new Position(COLUMNS - 2, ROWS - 2)
    );

    private final Tile[][] tiles;

    private GameMap(Tile[][] tiles) {
        this.tiles = copyAndValidate(tiles);
    }

    public static GameMap createDefault() {
        Tile[][] tiles = new Tile[ROWS][COLUMNS];
        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                if (isBorder(column, row) || isInternalHardWall(column, row)) {
                    tiles[row][column] = Tile.HARD_WALL;
                } else if ((column * 31 + row * 17) % 4 == 0) {
                    tiles[row][column] = Tile.BREAKABLE_WALL;
                } else {
                    tiles[row][column] = Tile.EMPTY;
                }
            }
        }

        for (Position spawn : SPAWN_POSITIONS) {
            clearSpawnArea(tiles, spawn);
        }
        return new GameMap(tiles);
    }

    /** Creates a fixed-size custom map, primarily useful for deterministic domain tests. */
    public static GameMap fromTiles(Tile[][] tiles) {
        return new GameMap(tiles);
    }

    public Tile getTile(Position position) {
        Objects.requireNonNull(position, "position must not be null");
        if (!isInside(position)) {
            throw new IllegalArgumentException("Position is outside the map: " + position);
        }
        return tiles[position.row()][position.column()];
    }

    public boolean isInside(Position position) {
        return position != null
                && position.column() >= 0
                && position.column() < COLUMNS
                && position.row() >= 0
                && position.row() < ROWS;
    }

    public boolean isWalkable(Position position) {
        return isInside(position) && getTile(position) == Tile.EMPTY;
    }

    public boolean destroyBreakableWall(Position position) {
        Objects.requireNonNull(position, "position must not be null");
        if (!isInside(position) || getTile(position) != Tile.BREAKABLE_WALL) {
            return false;
        }
        tiles[position.row()][position.column()] = Tile.EMPTY;
        return true;
    }

    public List<Position> getSpawnPositions() {
        return SPAWN_POSITIONS;
    }

    public List<List<Tile>> snapshotTiles() {
        return java.util.stream.IntStream.range(0, ROWS)
                .mapToObj(row -> java.util.stream.IntStream.range(0, COLUMNS)
                        .mapToObj(column -> tiles[row][column])
                        .toList())
                .toList();
    }

    private static boolean isBorder(int column, int row) {
        return column == 0 || column == COLUMNS - 1 || row == 0 || row == ROWS - 1;
    }

    private static boolean isInternalHardWall(int column, int row) {
        return column % 2 == 0 && row % 2 == 0;
    }

    private static void clearSpawnArea(Tile[][] tiles, Position spawn) {
        tiles[spawn.row()][spawn.column()] = Tile.EMPTY;
        for (Position neighbor : List.of(
                spawn.move(com.bomberman.common.enums.Direction.UP),
                spawn.move(com.bomberman.common.enums.Direction.DOWN),
                spawn.move(com.bomberman.common.enums.Direction.LEFT),
                spawn.move(com.bomberman.common.enums.Direction.RIGHT)
        )) {
            if (neighbor.column() > 0
                    && neighbor.column() < COLUMNS - 1
                    && neighbor.row() > 0
                    && neighbor.row() < ROWS - 1) {
                tiles[neighbor.row()][neighbor.column()] = Tile.EMPTY;
            }
        }
    }

    private static Tile[][] copyAndValidate(Tile[][] source) {
        Objects.requireNonNull(source, "tiles must not be null");
        if (source.length != ROWS) {
            throw new IllegalArgumentException("Map must have exactly " + ROWS + " rows");
        }

        Tile[][] copy = new Tile[ROWS][COLUMNS];
        for (int row = 0; row < ROWS; row++) {
            if (source[row] == null || source[row].length != COLUMNS) {
                throw new IllegalArgumentException(
                        "Every map row must have exactly " + COLUMNS + " columns"
                );
            }
            for (int column = 0; column < COLUMNS; column++) {
                copy[row][column] = Objects.requireNonNull(
                        source[row][column],
                        "Map tiles must not contain null"
                );
            }
        }
        return copy;
    }
}
