package com.bomberman.common.dto;

/**
 * @param lastMoveSeq highest {@link MoveRequest#seq()} of this player the server has applied or
 *                    rejected; the owning client drops its predicted moves up to it
 */
public record GamePlayerStateDto(
        long userId,
        String username,
        PositionDto position,
        boolean alive,
        int bombCapacity,
        int activeBombs,
        int bombRange,
        long lastMoveSeq
) {
}
