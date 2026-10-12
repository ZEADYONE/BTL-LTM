package com.bomberman.common.dto;

import com.bomberman.common.enums.RoomInviteStatus;

public record RoomInviteStatusUpdate(
        String inviteId,
        RoomInviteStatus status
) {
}
