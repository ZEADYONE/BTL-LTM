package com.bomberman.server.room;

public record RoomPlayerSnapshot(
        long userId,
        String username,
        String sessionId,
        boolean ready
) {
}
