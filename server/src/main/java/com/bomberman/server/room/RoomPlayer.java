package com.bomberman.server.room;

/** Mutable room membership, accessed only while RoomManager holds its lock. */
public final class RoomPlayer {

    private final long userId;
    private final String username;
    private final String sessionId;
    private boolean ready;

    RoomPlayer(long userId, String username, String sessionId) {
        this.userId = userId;
        this.username = username;
        this.sessionId = sessionId;
    }

    long getUserId() {
        return userId;
    }

    String getUsername() {
        return username;
    }

    String getSessionId() {
        return sessionId;
    }

    boolean isReady() {
        return ready;
    }

    void setReady(boolean ready) {
        this.ready = ready;
    }
}
