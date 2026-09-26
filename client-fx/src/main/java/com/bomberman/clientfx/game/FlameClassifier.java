package com.bomberman.clientfx.game;

import com.bomberman.common.dto.ExplosionStateDto;
import com.bomberman.common.dto.PositionDto;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Converts explosion positions into center/middle/end sprites and their rotation. */
public final class FlameClassifier {

    public enum Kind { CENTER, MID, END }

    public record Piece(PositionDto position, Kind kind, double rotationDegrees) {
    }

    private FlameClassifier() {
    }

    public static List<Piece> classify(ExplosionStateDto explosion) {
        PositionDto origin = explosion.origin();
        Set<PositionDto> affected = new HashSet<>(explosion.affectedPositions());
        return explosion.affectedPositions().stream().map(position -> {
            if (position.equals(origin)) {
                return new Piece(position, Kind.CENTER, 0);
            }
            int dc = Integer.compare(position.column(), origin.column());
            int dr = Integer.compare(position.row(), origin.row());
            PositionDto next = new PositionDto(position.column() + dc, position.row() + dr);
            Kind kind = affected.contains(next) ? Kind.MID : Kind.END;
            return new Piece(position, kind, rotation(dc, dr));
        }).toList();
    }

    private static double rotation(int dc, int dr) {
        if (dc > 0) return 0;
        if (dr > 0) return 90;
        if (dc < 0) return 180;
        return 270;
    }
}
