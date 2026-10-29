package com.bomberman.clientfx.game;

import com.bomberman.common.dto.ExplosionStateDto;
import com.bomberman.common.dto.PositionDto;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FlameClassifierTest {

    @Test
    void classifiesAFullRangeTwoCross() {
        PositionDto origin = p(3, 3);
        ExplosionStateDto explosion = new ExplosionStateDto(origin, List.of(
                origin, p(4, 3), p(5, 3), p(2, 3), p(1, 3),
                p(3, 4), p(3, 5), p(3, 2), p(3, 1)
        ), 500);

        List<FlameClassifier.Piece> pieces = FlameClassifier.classify(explosion);

        assertEquals(1, pieces.stream().filter(piece -> piece.kind() == FlameClassifier.Kind.CENTER).count());
        assertEquals(4, pieces.stream().filter(piece -> piece.kind() == FlameClassifier.Kind.MID).count());
        assertEquals(4, pieces.stream().filter(piece -> piece.kind() == FlameClassifier.Kind.END).count());
    }

    @Test
    void rotatesEndsTowardsAllFourDirections() {
        PositionDto origin = p(3, 3);
        ExplosionStateDto explosion = new ExplosionStateDto(origin,
                List.of(origin, p(4, 3), p(3, 4), p(2, 3), p(3, 2)), 500);

        Map<PositionDto, Double> rotations = FlameClassifier.classify(explosion).stream()
                .collect(Collectors.toMap(FlameClassifier.Piece::position, FlameClassifier.Piece::rotationDegrees));

        assertEquals(0, rotations.get(p(4, 3)));
        assertEquals(90, rotations.get(p(3, 4)));
        assertEquals(180, rotations.get(p(2, 3)));
        assertEquals(270, rotations.get(p(3, 2)));
    }

    private static PositionDto p(int column, int row) {
        return new PositionDto(column, row);
    }
}
