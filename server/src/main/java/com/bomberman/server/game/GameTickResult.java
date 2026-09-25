package com.bomberman.server.game;

import java.util.List;
import java.util.Set;

/** Changes produced by one authoritative game tick. */
public record GameTickResult(
        List<Explosion> explosions,
        Set<Position> destroyedWalls,
        Set<Long> diedPlayerIds,
        GameOutcome outcome
) {

    public GameTickResult {
        explosions = List.copyOf(explosions);
        destroyedWalls = Set.copyOf(destroyedWalls);
        diedPlayerIds = Set.copyOf(diedPlayerIds);
    }
}
