package com.bomberman.server.social;

import java.time.Instant;
import java.util.UUID;

/** Immutable, in-memory invitation bound to one room and recipient. */
public record RoomInvite(
        UUID inviteId,
        String roomId,
        String roomName,
        long inviterUserId,
        String inviterUsername,
        long inviteeUserId,
        Instant createdAt,
        Instant expiresAt
) {
}
