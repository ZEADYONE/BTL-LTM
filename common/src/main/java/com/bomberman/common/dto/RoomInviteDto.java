package com.bomberman.common.dto;

/** Invitation received by a free, online friend. */
public record RoomInviteDto(
        String inviteId,
        String roomId,
        String roomName,
        long inviterUserId,
        String inviterUsername,
        long expiresAtEpochMillis
) {
}
