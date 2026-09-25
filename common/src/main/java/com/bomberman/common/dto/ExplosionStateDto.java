package com.bomberman.common.dto;

import java.util.List;

public record ExplosionStateDto(
        PositionDto origin,
        List<PositionDto> affectedPositions,
        long remainingMillis
) {

    public ExplosionStateDto {
        affectedPositions = List.copyOf(affectedPositions);
    }
}
