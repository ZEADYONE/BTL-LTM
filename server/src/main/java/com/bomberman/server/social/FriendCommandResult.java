package com.bomberman.server.social;

import com.bomberman.common.enums.FriendResultCode;

public record FriendCommandResult(FriendResultCode error, Long otherUserId) {

    public static FriendCommandResult success(long otherUserId) {
        return new FriendCommandResult(null, otherUserId);
    }

    public static FriendCommandResult failure(FriendResultCode error) {
        return new FriendCommandResult(error, null);
    }

    public boolean isSuccess() {
        return error == null;
    }
}
