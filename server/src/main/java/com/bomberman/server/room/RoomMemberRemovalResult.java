package com.bomberman.server.room;

import com.bomberman.common.enums.RoomResultCode;

/** Result of an atomic room-member removal, including the removed session for notifications. */
public record RoomMemberRemovalResult(
        RoomResultCode result,
        RoomSnapshot room,
        RoomPlayerSnapshot removedPlayer
) {

    public boolean isSuccess() {
        return result == RoomResultCode.SUCCESS;
    }
}
