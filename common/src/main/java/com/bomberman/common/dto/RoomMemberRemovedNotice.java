package com.bomberman.common.dto;

import com.bomberman.common.enums.RoomRemovalReason;

public record RoomMemberRemovedNotice(
        String roomId,
        String roomName,
        RoomRemovalReason reason,
        long actorUserId,
        String actorUsername
) {
}
