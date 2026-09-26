package com.bomberman.clientfx;

import java.util.List;

/**
 * Server-side rules the client relies on for validation and presentation.
 * Keep in sync with the server; sources are listed in docs/ui-redesign/03-kien-truc-ky-thuat.md, section 9.
 */
public final class ServerRules {

    /** A tile position; column 0 is the left edge, row 0 the top edge. */
    public record Cell(int column, int row) {
    }

    /** AuthenticationService.MAX_USERNAME_LENGTH. */
    public static final int MAX_USERNAME_LENGTH = 50;

    /** RoomManager.MAX_ROOM_NAME_LENGTH (after trimming). */
    public static final int MAX_ROOM_NAME_LENGTH = 60;

    /** BombermanGame.MIN_PLAYERS / MAX_PLAYERS. */
    public static final int MIN_PLAYERS = 2;
    public static final int MAX_PLAYERS = 4;

    /** GameMap.COLUMNS / ROWS. */
    public static final int MAP_COLUMNS = 13;
    public static final int MAP_ROWS = 11;

    /** RoomGameLoop.TICKS_PER_SECOND; snapshots currently arrive every second tick. */
    public static final int TICKS_PER_SECOND = 20;
    public static final int SNAPSHOTS_PER_SECOND = 10;

    /** BombermanGame.BOMB_FUSE / EXPLOSION_DURATION. */
    public static final long BOMB_FUSE_MILLIS = 3_000;
    public static final long EXPLOSION_MILLIS = 500;

    /** GameMap.SPAWN_POSITIONS; player i of the room starts on spawn i. */
    public static final List<Cell> SPAWNS = List.of(
            new Cell(1, 1),
            new Cell(MAP_COLUMNS - 2, 1),
            new Cell(1, MAP_ROWS - 2),
            new Cell(MAP_COLUMNS - 2, MAP_ROWS - 2)
    );

    private ServerRules() {
    }
}
