package com.bomberman.server.game;

import com.bomberman.common.enums.GameStatus;

import java.util.List;

/** Immutable internal snapshot captured by the single game-loop thread. */
public record BombermanSnapshot(
        long tick,
        GameStatus status,
        List<List<Tile>> map,
        List<PlayerSnapshot> players,
        List<BombSnapshot> bombs,
        List<ExplosionSnapshot> explosions,
        int remainingPlayers
) {

    public BombermanSnapshot {
        map = map.stream().map(List::copyOf).toList();
        players = List.copyOf(players);
        bombs = List.copyOf(bombs);
        explosions = List.copyOf(explosions);
    }

    public record PlayerSnapshot(
            long userId,
            String username,
            Position position,
            boolean alive,
            int bombCapacity,
            int activeBombs,
            int bombRange
    ) {
    }

    public record BombSnapshot(
            String bombId,
            long ownerUserId,
            Position position,
            int blastRange,
            long remainingFuseMillis
    ) {
    }

    public record ExplosionSnapshot(
            Position origin,
            List<Position> affectedPositions,
            long remainingMillis
    ) {

        public ExplosionSnapshot {
            affectedPositions = List.copyOf(affectedPositions);
        }
    }
}
