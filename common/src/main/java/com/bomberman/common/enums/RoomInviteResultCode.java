package com.bomberman.common.enums;

/** Machine-readable error result of a room invitation command. */
public enum RoomInviteResultCode {
    INVALID_REQUEST,
    NOT_AUTHENTICATED,
    NOT_FRIENDS,
    INVITER_NOT_IN_ROOM,
    ROOM_NOT_WAITING,
    INVITEE_OFFLINE,
    INVITEE_BUSY,
    DUPLICATE_INVITE,
    INVITE_NOT_FOUND,
    INVITE_EXPIRED,
    INVITE_NOT_RECIPIENT,
    ROOM_FULL,
    TARGET_KICKED,
    RATE_LIMITED
}
