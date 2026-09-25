package com.bomberman.common.dto;

public record BombStateDto(
        String bombId,
        long ownerUserId,
        PositionDto position,
        int blastRange,
        long remainingFuseMillis
) {
}
