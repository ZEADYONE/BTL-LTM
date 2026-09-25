package com.bomberman.server.game;

import java.time.Instant;
import java.util.Set;

/** Immutable explosion snapshot; propagation behavior is implemented in a later stage. */
public record Explosion(
        Position origin,
        Set<Position> affectedPositions,
        Instant startedAt,
        Instant expiresAt
) {

    public Explosion {
        affectedPositions = Set.copyOf(affectedPositions);
        if (expiresAt.isBefore(startedAt)) {
            throw new IllegalArgumentException("expiresAt must not be before startedAt");
        }
    }
}
