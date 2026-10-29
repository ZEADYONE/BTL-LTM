package com.bomberman.server.social;

import com.bomberman.common.enums.RoomInviteResultCode;

public record RoomInviteCommandResult(RoomInviteResultCode result, RoomInvite invite) {

    public static RoomInviteCommandResult success(RoomInvite invite) {
        return new RoomInviteCommandResult(null, invite);
    }

    public static RoomInviteCommandResult failure(RoomInviteResultCode result) {
        return new RoomInviteCommandResult(result, null);
    }

    public boolean isSuccess() {
        return result == null;
    }
}
