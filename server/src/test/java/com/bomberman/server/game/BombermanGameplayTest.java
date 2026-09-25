package com.bomberman.server.game;

import com.bomberman.common.enums.Direction;
import com.bomberman.common.enums.GameResult;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BombermanGameplayTest {

    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void bombExplodesAfterThreeSecondsAndReleasesCapacity() {
        BomberPlayer owner = player(1, 5, 5);
        BombermanGame game = game(emptyMap(), owner, player(2, 11, 9));

        assertEquals(PlaceBombResult.PLACED, game.placeBomb(1, START));
        GameTickResult earlyTick = game.tick(START.plusSeconds(2));

        assertTrue(earlyTick.explosions().isEmpty());
        assertEquals(1, game.getBombCount());
        assertEquals(1, owner.getActiveBombs());

        GameTickResult detonationTick = game.tick(START.plusSeconds(3));

        assertEquals(1, detonationTick.explosions().size());
        assertEquals(0, game.getBombCount());
        assertEquals(0, owner.getActiveBombs());
    }

    @Test
    void bombCapacityAndOccupiedTileAreEnforced() {
        BomberPlayer owner = player(1, 5, 5);
        BombermanGame game = game(emptyMap(), owner, player(2, 11, 9));

        assertEquals(PlaceBombResult.PLACED, game.placeBomb(1, START));
        assertEquals(PlaceBombResult.BOMB_ALREADY_PRESENT, game.placeBomb(1, START));
        assertEquals(MoveResult.MOVED, game.movePlayer(1, Direction.RIGHT));
        assertEquals(PlaceBombResult.BOMB_CAPACITY_REACHED, game.placeBomb(1, START));
    }

    @Test
    void explosionSpreadsTwoTilesInFourDirections() {
        BombermanGame game = game(emptyMap(), player(1, 6, 5), player(2, 11, 9));
        game.placeBomb(1, START);

        Explosion explosion = game.tick(START.plusSeconds(3)).explosions().getFirst();

        assertEquals(9, explosion.affectedPositions().size());
        for (Direction direction : Direction.values()) {
            assertTrue(explosion.affectedPositions().contains(
                    new Position(6, 5).move(direction)
            ));
            assertTrue(explosion.affectedPositions().contains(
                    new Position(6, 5).move(direction).move(direction)
            ));
        }
    }

    @Test
    void hardWallStopsExplosionAndIsNotDestroyed() {
        Tile[][] tiles = emptyTiles();
        Position hardWall = new Position(7, 5);
        tiles[hardWall.row()][hardWall.column()] = Tile.HARD_WALL;
        GameMap map = GameMap.fromTiles(tiles);
        BombermanGame game = game(map, player(1, 5, 5), player(2, 11, 9));
        game.placeBomb(1, START);

        GameTickResult tick = game.tick(START.plusSeconds(3));
        Explosion explosion = tick.explosions().getFirst();

        assertTrue(explosion.affectedPositions().contains(new Position(6, 5)));
        assertFalse(explosion.affectedPositions().contains(hardWall));
        assertEquals(Tile.HARD_WALL, map.getTile(hardWall));
        assertFalse(tick.destroyedWalls().contains(hardWall));
    }

    @Test
    void breakableWallIsDestroyedAndStopsExplosion() {
        Tile[][] tiles = emptyTiles();
        Position breakableWall = new Position(6, 5);
        tiles[breakableWall.row()][breakableWall.column()] = Tile.BREAKABLE_WALL;
        GameMap map = GameMap.fromTiles(tiles);
        BombermanGame game = game(map, player(1, 5, 5), player(2, 11, 9));
        game.placeBomb(1, START);

        GameTickResult tick = game.tick(START.plusSeconds(3));
        Explosion explosion = tick.explosions().getFirst();

        assertTrue(explosion.affectedPositions().contains(breakableWall));
        assertFalse(explosion.affectedPositions().contains(new Position(7, 5)));
        assertEquals(Tile.EMPTY, map.getTile(breakableWall));
        assertTrue(tick.destroyedWalls().contains(breakableWall));
    }

    @Test
    void explosionKillsAnotherPlayer() {
        BomberPlayer owner = player(1, 5, 5);
        BomberPlayer victim = player(2, 6, 5);
        BombermanGame game = game(emptyMap(), owner, victim);
        game.placeBomb(1, START);

        GameTickResult tick = game.tick(START.plusSeconds(3));

        assertFalse(victim.isAlive());
        assertTrue(tick.diedPlayerIds().contains(victim.getUserId()));
    }

    @Test
    void ownerCanBeKilledByOwnBomb() {
        BomberPlayer owner = player(1, 5, 5);
        BombermanGame game = game(emptyMap(), owner, player(2, 11, 9));
        game.placeBomb(1, START);

        game.tick(START.plusSeconds(3));

        assertFalse(owner.isAlive());
    }

    @Test
    void explosionTriggersAnotherBombImmediatelyOnlyOnce() {
        BomberPlayer first = player(1, 5, 5);
        BomberPlayer second = player(2, 6, 5);
        BombermanGame game = game(emptyMap(), first, second);
        game.placeBomb(1, START);
        game.placeBomb(2, START.plusSeconds(1));

        GameTickResult tick = game.tick(START.plusSeconds(3));

        assertEquals(2, tick.explosions().size());
        assertEquals(0, game.getBombCount());
        assertEquals(0, first.getActiveBombs());
        assertEquals(0, second.getActiveBombs());
    }

    @Test
    void lastAlivePlayerWins() {
        BomberPlayer owner = player(1, 5, 5);
        BomberPlayer victim = player(2, 6, 5);
        BombermanGame game = game(emptyMap(), owner, victim);
        game.placeBomb(1, START);
        game.movePlayer(1, Direction.LEFT);
        game.movePlayer(1, Direction.LEFT);
        game.movePlayer(1, Direction.LEFT);

        GameOutcome outcome = game.tick(START.plusSeconds(3)).outcome();

        assertTrue(owner.isAlive());
        assertFalse(victim.isAlive());
        assertEquals(GameResult.WIN, outcome.result());
        assertEquals(owner.getUserId(), outcome.winnerUserId());
    }

    @Test
    void simultaneousDeathsProduceDraw() {
        BomberPlayer first = player(1, 5, 5);
        BomberPlayer second = player(2, 6, 5);
        BombermanGame game = game(emptyMap(), first, second);
        game.placeBomb(1, START);

        GameOutcome outcome = game.tick(START.plusSeconds(3)).outcome();

        assertFalse(first.isAlive());
        assertFalse(second.isAlive());
        assertEquals(GameResult.DRAW, outcome.result());
        assertNull(outcome.winnerUserId());
    }

    private BombermanGame game(GameMap map, BomberPlayer... players) {
        return new BombermanGame(map, List.of(players));
    }

    private BomberPlayer player(long userId, int column, int row) {
        return new BomberPlayer(userId, "user-" + userId, new Position(column, row));
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
