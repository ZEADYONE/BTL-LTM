package com.bomberman.common.enums;

/**
 * Identifies the semantic type of a message inside a TCP frame.
 */
public enum MessageType {
    REGISTER_REQUEST,
    REGISTER_RESPONSE,
    LOGIN_REQUEST,
    LOGIN_RESPONSE,
    LOGOUT,
    ONLINE_USERS_REQUEST,
    ONLINE_USERS_UPDATE,
    ROOM_LIST_REQUEST,
    ROOM_LIST_UPDATE,
    CREATE_ROOM,
    JOIN_ROOM,
    LEAVE_ROOM,
    ROOM_STATE,
    READY,
    START_GAME,
    MOVE,
    PLACE_BOMB,
    GAME_STATE,
    PLAYER_DIED,
    GAME_OVER,
    PLAY_AGAIN,
    RANKING_REQUEST,
    RANKING_RESPONSE,
    HISTORY_REQUEST,
    HISTORY_RESPONSE,
    ERROR,
    PING,
    PONG
}
