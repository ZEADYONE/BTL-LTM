package com.bomberman.server.game;

import com.bomberman.common.enums.Direction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BombermanMovementTest {

    @Test
    void playerHasRequiredDefaults() {
        BomberPlayer player = player(1, new Position(5, 5));

        assertTrue(player.isAlive());
        assertEquals(1, player.getBombCapacity());
        assertEquals(0, player.getActiveBombs());
        assertEquals(2, player.getBombRange());
    }

    @ParameterizedTest
    @EnumSource(Direction.class)
    void movesExactlyOneTileInEveryDirection(Direction direction) {
        Position start = new Position(6, 5);
        BomberPlayer player = player(1, start);
        BombermanGame game = game(emptyMap(), player, player(2, new Position(10, 9)));

        MoveResult result = game.movePlayer(player.getUserId(), direction);

        assertEquals(MoveResult.MOVED, result);
        assertEquals(start.move(direction), player.getPosition());
    }

    @ParameterizedTest
    @EnumSource(value = Tile.class, names = {"HARD_WALL", "BREAKABLE_WALL"})
    void cannotMoveIntoWall(Tile wall) {
        Tile[][] tiles = emptyTiles();
        tiles[5][6] = wall;
        BomberPlayer player = player(1, new Position(5, 5));
        BombermanGame game = game(GameMap.fromTiles(tiles), player, player(2, new Position(10, 9)));

        MoveResult result = game.movePlayer(player.getUserId(), Direction.RIGHT);

        assertEquals(MoveResult.BLOCKED_BY_WALL, result);
        assertEquals(new Position(5, 5), player.getPosition());
    }

    @Test
    void cannotMoveOutsideMap() {
        BomberPlayer player = player(1, new Position(0, 0));
        BombermanGame game = game(emptyMap(), player, player(2, new Position(10, 9)));

        MoveResult result = game.movePlayer(player.getUserId(), Direction.UP);

        assertEquals(MoveResult.OUT_OF_BOUNDS, result);
        assertEquals(new Position(0, 0), player.getPosition());
    }

    @Test
    void bombAllowsPlayerToLeaveButBlocksReentry() {
        Position bombPosition = new Position(5, 5);
        BomberPlayer owner = player(1, bombPosition);
        Instant placedAt = Instant.parse("2026-01-01T00:00:00Z");
        Bomb bomb = Bomb.scheduled(
                owner.getUserId(),
                bombPosition,
                owner.getBombRange(),
                placedAt,
                placedAt.plusSeconds(3)
        );
        BombermanGame game = new BombermanGame(
                emptyMap(),
                List.of(owner, player(2, new Position(10, 9))),
                List.of(bomb)
        );

        assertEquals(MoveResult.MOVED, game.movePlayer(owner.getUserId(), Direction.RIGHT));
        assertEquals(MoveResult.BLOCKED_BY_BOMB, game.movePlayer(owner.getUserId(), Direction.LEFT));
        assertEquals(new Position(6, 5), owner.getPosition());
    }

    @Test
    void cannotMoveIntoAnotherAlivePlayer() {
        BomberPlayer first = player(1, new Position(5, 5));
        BomberPlayer second = player(2, new Position(6, 5));
        BombermanGame game = game(emptyMap(), first, second);

        assertEquals(MoveResult.BLOCKED_BY_PLAYER, game.movePlayer(1, Direction.RIGHT));
        assertEquals(new Position(5, 5), first.getPosition());
    }

    @Test
    void deadPlayerCannotMove() {
        BomberPlayer player = player(1, new Position(5, 5));
        BombermanGame game = game(emptyMap(), player, player(2, new Position(10, 9)));
        player.kill();

        assertEquals(MoveResult.PLAYER_DEAD, game.movePlayer(1, Direction.RIGHT));
        assertFalse(player.isAlive());
        assertEquals(new Position(5, 5), player.getPosition());
    }

    @Test
    void unknownPlayerCannotMove() {
        BombermanGame game = game(
                emptyMap(),
                player(1, new Position(5, 5)),
                player(2, new Position(10, 9))
        );

        assertEquals(MoveResult.PLAYER_NOT_FOUND, game.movePlayer(999, Direction.RIGHT));
    }

    @Test
    void defaultGameAssignsParticipantsToFixedSpawns() {
        BombermanGame game = BombermanGame.createDefault(List.of(
                new GameParticipant(1, "first"),
                new GameParticipant(2, "second"),
                new GameParticipant(3, "third"),
                new GameParticipant(4, "fourth")
        ));

        assertEquals(new Position(1, 1), game.getPlayer(1).getPosition());
        assertEquals(new Position(11, 1), game.getPlayer(2).getPosition());
        assertEquals(new Position(1, 9), game.getPlayer(3).getPosition());
        assertEquals(new Position(11, 9), game.getPlayer(4).getPosition());
    }

    private BombermanGame game(GameMap map, BomberPlayer first, BomberPlayer second) {
        return new BombermanGame(map, List.of(first, second));
    }

    private BomberPlayer player(long userId, Position position) {
        return new BomberPlayer(userId, "user-" + userId, position);
    }

    private GameMap emptyMap() {
        return GameMap.fromTiles(emptyTiles());
    }

    private Tile[][] emptyTiles() {
        Tile[][] tiles = new Tile[GameMap.ROWS][GameMap.COLUMNS];
        for (Tile[] row : tiles) {
            Arrays.fill(row, Tile.EMPTY);
        }
        return tiles;
    }
}
