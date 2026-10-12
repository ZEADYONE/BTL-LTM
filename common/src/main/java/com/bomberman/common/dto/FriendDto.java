package com.bomberman.common.dto;

import com.bomberman.common.enums.FriendPresenceStatus;

public record FriendDto(
        long userId,
        String username,
        FriendPresenceStatus presence
) {
}
