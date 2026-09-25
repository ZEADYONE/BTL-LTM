package com.bomberman.common.dto;

import com.bomberman.common.enums.RoomStatus;

import java.util.List;

public record RoomStateDto(
        String roomId,
        String roomName,
        long hostUserId,
        List<RoomPlayerDto> players,
        int maxPlayers,
        RoomStatus status
) {

    public RoomStateDto {
        players = List.copyOf(players);
    }
}
