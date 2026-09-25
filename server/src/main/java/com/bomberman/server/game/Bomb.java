package com.bomberman.server.game;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Immutable bomb state; detonation behavior is intentionally not implemented yet. */
public record Bomb(
        String bombId,
        long ownerUserId,
        Position position,
        int blastRange,
        Instant placedAt,
        Instant detonateAt
) {

    public Bomb {
        Objects.requireNonNull(bombId, "bombId must not be null");
        Objects.requireNonNull(position, "position must not be null");
        Objects.requireNonNull(placedAt, "placedAt must not be null");
        Objects.requireNonNull(detonateAt, "detonateAt must not be null");
        if (blastRange < 1) {
            throw new IllegalArgumentException("blastRange must be positive");
        }
        if (detonateAt.isBefore(placedAt)) {
            throw new IllegalArgumentException("detonateAt must not be before placedAt");
        }
    }

    public static Bomb scheduled(
            long ownerUserId,
            Position position,
            int blastRange,
            Instant placedAt,
            Instant detonateAt
    ) {
        return new Bomb(
                UUID.randomUUID().toString(),
                ownerUserId,
                position,
                blastRange,
                placedAt,
                detonateAt
        );
    }
}
