package com.bomberman.common.dto;

public record GamePlayerStateDto(
        long userId,
        String username,
        PositionDto position,
        boolean alive,
        int bombCapacity,
        int activeBombs,
        int bombRange
) {
}
