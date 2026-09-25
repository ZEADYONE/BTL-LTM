package com.bomberman.common.enums;

/** Machine-readable result of a room command. */
public enum RoomResultCode {
    SUCCESS,
    INVALID_REQUEST,
    NOT_AUTHENTICATED,
    INVALID_ROOM_NAME,
    ALREADY_IN_ROOM,
    ROOM_NOT_FOUND,
    ROOM_NOT_WAITING,
    ROOM_NOT_FINISHED,
    ROOM_FULL,
    NOT_IN_ROOM,
    NOT_HOST,
    NOT_ENOUGH_PLAYERS,
    NOT_ALL_PLAYERS_READY
}
