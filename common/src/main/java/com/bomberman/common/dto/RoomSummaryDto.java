package com.bomberman.common.dto;

import com.bomberman.common.enums.RoomStatus;

public record RoomSummaryDto(
        String roomId,
        String roomName,
        long hostUserId,
        int playerCount,
        int maxPlayers,
        RoomStatus status
) {
}
