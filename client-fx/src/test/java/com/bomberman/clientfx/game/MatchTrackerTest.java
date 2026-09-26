package com.bomberman.clientfx.game;

import com.bomberman.common.dto.BombStateDto;
import com.bomberman.common.dto.ExplosionStateDto;
import com.bomberman.common.dto.GamePlayerStateDto;
import com.bomberman.common.dto.GameOverDto;
import com.bomberman.common.dto.GameOverPlayerDto;
import com.bomberman.common.dto.GameStateDto;
import com.bomberman.common.dto.PositionDto;
import com.bomberman.common.enums.GameStatus;
import com.bomberman.common.enums.GameResult;
import com.bomberman.common.enums.TileType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchTrackerTest {

    @Test
    void fixesSlotsAndRecordsEliminationsAtTheSnapshotTick() {
        MatchTracker tracker = new MatchTracker();
        tracker.accept(snapshot(2, players(true, true), map(TileType.BREAKABLE_WALL), List.of(), List.of()));

        MatchTracker.FrameEvents events = tracker.accept(
                snapshot(4, players(false, false), map(TileType.EMPTY), List.of(), List.of()));

        assertEquals(0, tracker.slotFor(11).orElseThrow());
        assertEquals(1, tracker.slotFor(22).orElseThrow());
        assertEquals(List.of(11L, 22L), events.eliminatedPlayers());
        assertEquals(4L, tracker.eliminationTicks().get(11L));
        assertEquals(4L, tracker.eliminationTicks().get(22L));
        assertEquals(List.of(new PositionDto(0, 0)), events.brokenCrates());
    }

    @Test
    void detectsNewBombsExplosionsAndMovementOnlyOnce() {
        MatchTracker tracker = new MatchTracker();
        tracker.accept(snapshot(2, players(true, true), map(TileType.EMPTY), List.of(), List.of()));
        BombStateDto bomb = new BombStateDto("b1", 11, new PositionDto(1, 1), 2, 2000);
        ExplosionStateDto explosion = new ExplosionStateDto(
                new PositionDto(2, 2), List.of(new PositionDto(2, 2)), 500);
        List<GamePlayerStateDto> moved = List.of(player(11, 2, 1, true), player(22, 3, 3, true));

        MatchTracker.FrameEvents first = tracker.accept(
                snapshot(4, moved, map(TileType.EMPTY), List.of(bomb), List.of(explosion)));
        MatchTracker.FrameEvents second = tracker.accept(
                snapshot(6, moved, map(TileType.EMPTY), List.of(bomb), List.of(explosion)));

        assertEquals(List.of(11L), first.movedPlayers());
        assertEquals(1, first.newBombs().size());
        assertEquals(1, first.newExplosions().size());
        assertTrue(second.newBombs().isEmpty());
        assertTrue(second.newExplosions().isEmpty());
    }

    @Test
    void lowerTickStartsANewMatchAndClearsOldEliminations() {
        MatchTracker tracker = new MatchTracker();
        tracker.accept(snapshot(20, players(false, true), map(TileType.EMPTY), List.of(), List.of()));

        MatchTracker.FrameEvents events = tracker.accept(
                snapshot(2, players(true, true), map(TileType.EMPTY), List.of(), List.of()));

        assertTrue(events.newMatch());
        assertTrue(tracker.eliminationTicks().isEmpty());
    }

    @Test
    void ranksLaterEliminationsHigherAndKeepsSameTickTied() {
        MatchTracker tracker = new MatchTracker();
        List<GamePlayerStateDto> initial = List.of(
                player(11, 1, 1, true), player(22, 3, 3, true),
                player(33, 5, 5, true), player(44, 7, 7, true));
        tracker.accept(snapshot(2, initial, map(TileType.EMPTY), List.of(), List.of()));
        tracker.accept(snapshot(4, List.of(
                player(11, 1, 1, true), player(22, 3, 3, false),
                player(33, 5, 5, true), player(44, 7, 7, true)),
                map(TileType.EMPTY), List.of(), List.of()));
        tracker.accept(snapshot(6, List.of(
                player(11, 1, 1, true), player(22, 3, 3, false),
                player(33, 5, 5, false), player(44, 7, 7, false)),
                map(TileType.EMPTY), List.of(), List.of()));
        GameOverDto gameOver = new GameOverDto(11L, GameResult.WIN, List.of(
                result(11, GameResult.WIN), result(22, GameResult.LOSS),
                result(33, GameResult.LOSS), result(44, GameResult.LOSS)));

        List<MatchTracker.RankedPlayer> ranked = tracker.rank(gameOver);

        assertEquals(List.of(11L, 33L, 44L, 22L),
                ranked.stream().map(MatchTracker.RankedPlayer::userId).toList());
        assertEquals(List.of(1, 2, 2, 4), ranked.stream().map(MatchTracker.RankedPlayer::rank).toList());
    }

    @Test
    void drawGivesEveryPlayerRankOne() {
        MatchTracker tracker = new MatchTracker();
        tracker.accept(snapshot(2, players(true, true), map(TileType.EMPTY), List.of(), List.of()));
        GameOverDto draw = new GameOverDto(null, GameResult.DRAW,
                List.of(result(11, GameResult.DRAW), result(22, GameResult.DRAW)));

        assertEquals(List.of(1, 1), tracker.rank(draw).stream().map(MatchTracker.RankedPlayer::rank).toList());
    }

    @Test
    void missingEliminationDataFallsBackToResultThenSlot() {
        MatchTracker tracker = new MatchTracker();
        tracker.accept(snapshot(2, players(true, true), map(TileType.EMPTY), List.of(), List.of()));
        GameOverDto gameOver = new GameOverDto(22L, GameResult.WIN,
                List.of(result(22, GameResult.WIN), result(11, GameResult.LOSS)));

        List<MatchTracker.RankedPlayer> ranked = tracker.rank(gameOver);

        assertEquals(List.of(22L, 11L), ranked.stream().map(MatchTracker.RankedPlayer::userId).toList());
        assertEquals(List.of(1, 2), ranked.stream().map(MatchTracker.RankedPlayer::rank).toList());
    }

    private static List<GamePlayerStateDto> players(boolean firstAlive, boolean secondAlive) {
        return List.of(player(11, 1, 1, firstAlive), player(22, 3, 3, secondAlive));
    }

    private static GamePlayerStateDto player(long id, int column, int row, boolean alive) {
        return new GamePlayerStateDto(id, "p" + id, new PositionDto(column, row), alive, 1, 0, 2);
    }

    private static GameOverPlayerDto result(long userId, GameResult result) {
        return new GameOverPlayerDto(userId, result, result == GameResult.WIN ? 2 : result == GameResult.DRAW ? 1 : 0);
    }

    private static List<List<TileType>> map(TileType tile) {
        return List.of(List.of(tile));
    }

    private static GameStateDto snapshot(long tick, List<GamePlayerStateDto> players,
                                         List<List<TileType>> map, List<BombStateDto> bombs,
                                         List<ExplosionStateDto> explosions) {
        return new GameStateDto(tick, GameStatus.RUNNING, map, players, bombs, explosions,
                (int) players.stream().filter(GamePlayerStateDto::alive).count());
    }
}
