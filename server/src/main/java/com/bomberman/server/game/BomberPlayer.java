package com.bomberman.server.game;

import java.util.Objects;

/** Authoritative mutable state of one player in a game. */
public final class BomberPlayer {

    public static final int DEFAULT_BOMB_CAPACITY = 1;
    public static final int DEFAULT_BOMB_RANGE = 2;

    private final long userId;
    private final String username;
    private Position position;
    private boolean alive = true;
    private int bombCapacity = DEFAULT_BOMB_CAPACITY;
    private int activeBombs;
    private int bombRange = DEFAULT_BOMB_RANGE;

    public BomberPlayer(long userId, String username, Position position) {
        this.userId = userId;
        this.username = Objects.requireNonNull(username, "username must not be null");
        this.position = Objects.requireNonNull(position, "position must not be null");
    }

    public long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public Position getPosition() {
        return position;
    }

    public boolean isAlive() {
        return alive;
    }

    public int getBombCapacity() {
        return bombCapacity;
    }

    public int getActiveBombs() {
        return activeBombs;
    }

    public int getBombRange() {
        return bombRange;
    }

    public void kill() {
        alive = false;
    }

    boolean canPlaceBomb() {
        return activeBombs < bombCapacity;
    }

    void bombPlaced() {
        if (!canPlaceBomb()) {
            throw new IllegalStateException("Bomb capacity exceeded");
        }
        activeBombs++;
    }

    void bombExploded() {
        if (activeBombs > 0) {
            activeBombs--;
        }
    }

    void moveTo(Position position) {
        this.position = Objects.requireNonNull(position);
    }
}
