package com.bomberman.common.enums;

/** Machine-readable result of a friend command. */
public enum FriendResultCode {
    INVALID_REQUEST,
    NOT_AUTHENTICATED,
    USER_NOT_FOUND,
    CANNOT_ADD_SELF,
    ALREADY_FRIENDS,
    REQUEST_ALREADY_PENDING,
    REQUEST_NOT_FOUND,
    NOT_REQUEST_RECIPIENT,
    NOT_REQUEST_SENDER,
    NOT_FRIENDS,
    FRIEND_LIMIT_REACHED,
    PENDING_LIMIT_REACHED,
    RATE_LIMITED
}
