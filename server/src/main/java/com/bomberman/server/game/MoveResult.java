package com.bomberman.server.game;

public enum MoveResult {
    MOVED,
    GAME_OVER,
    PLAYER_NOT_FOUND,
    PLAYER_DEAD,
    OUT_OF_BOUNDS,
    BLOCKED_BY_WALL,
    BLOCKED_BY_BOMB,
    BLOCKED_BY_PLAYER
}
