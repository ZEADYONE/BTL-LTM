package com.bomberman.common.dto;

import com.bomberman.common.enums.FriendPresenceStatus;

public record FriendRequestDto(
        long relationshipId,
        long userId,
        String username,
        FriendPresenceStatus presence,
        long requestedAtEpochMillis
) {
}
