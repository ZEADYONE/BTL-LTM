package com.bomberman.server.room;

import com.bomberman.common.enums.RoomStatus;

import java.util.List;

public record RoomSnapshot(
        String roomId,
        String roomName,
        long hostUserId,
        List<RoomPlayerSnapshot> players,
        int maxPlayers,
        RoomStatus status
) {

    public RoomSnapshot {
        players = List.copyOf(players);
    }
}
