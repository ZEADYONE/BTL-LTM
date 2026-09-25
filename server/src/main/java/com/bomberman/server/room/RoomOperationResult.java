package com.bomberman.server.room;

import com.bomberman.common.enums.RoomResultCode;

public record RoomOperationResult(RoomResultCode result, RoomSnapshot room) {

    public boolean isSuccess() {
        return result == RoomResultCode.SUCCESS;
    }
}
