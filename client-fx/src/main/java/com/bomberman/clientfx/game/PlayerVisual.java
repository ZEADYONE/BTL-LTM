package com.bomberman.clientfx.game;

import com.bomberman.common.dto.PositionDto;

import java.util.Objects;

/** Smooth display position and facing for one authoritative player. Units are map tiles. */
public final class PlayerVisual {

    public enum Facing { DOWN, UP, LEFT, RIGHT }

    public static final double VISUAL_TILE_MILLIS = 130;
    public static final double SNAP_DISTANCE = 2.5;

    private double x;
    private double y;
    private double targetX;
    private double targetY;
    private Facing facing = Facing.DOWN;
    private boolean moving;
    private boolean alive = true;
    private long deathNanos = Long.MIN_VALUE;

    public PlayerVisual(PositionDto initial) {
        Objects.requireNonNull(initial, "initial must not be null");
        x = targetX = initial.column();
        y = targetY = initial.row();
    }

    public void setTarget(PositionDto target) {
        Objects.requireNonNull(target, "target must not be null");
        double dx = target.column() - targetX;
        double dy = target.row() - targetY;
        if (Math.abs(dx) >= Math.abs(dy) && dx != 0) {
            facing = dx > 0 ? Facing.RIGHT : Facing.LEFT;
        } else if (dy != 0) {
            facing = dy > 0 ? Facing.DOWN : Facing.UP;
        }
        targetX = target.column();
        targetY = target.row();
    }

    public void advance(double elapsedSeconds) {
        if (elapsedSeconds <= 0) {
            moving = distanceToTarget() > 0.001;
            return;
        }
        double dx = targetX - x;
        double dy = targetY - y;
        double distance = Math.hypot(dx, dy);
        if (distance > SNAP_DISTANCE) {
            x = targetX;
            y = targetY;
            moving = false;
            return;
        }
        if (distance <= 0.001) {
            x = targetX;
            y = targetY;
            moving = false;
            return;
        }
        double speed = 1_000.0 / VISUAL_TILE_MILLIS;
        if (distance > 1) {
            speed *= 2;
        }
        double step = Math.min(distance, speed * elapsedSeconds);
        x += dx / distance * step;
        y += dy / distance * step;
        moving = step < distance;
    }

    public void setAlive(boolean alive, long nowNanos) {
        if (this.alive && !alive) {
            deathNanos = nowNanos;
        }
        this.alive = alive;
    }

    public double walkBob(long nowNanos) {
        return moving ? Math.sin(nowNanos / 1_000_000_000.0 * Math.PI * 2 / 0.260) * 2 : 0;
    }

    public double walkTilt(long nowNanos) {
        return moving ? Math.sin(nowNanos / 1_000_000_000.0 * Math.PI * 2 / 0.260) * 3 : 0;
    }

    public double deathProgress(long nowNanos) {
        if (alive || deathNanos == Long.MIN_VALUE) {
            return 0;
        }
        return Math.min(1, Math.max(0, (nowNanos - deathNanos) / 600_000_000.0));
    }

    public double distanceToTarget() {
        return Math.hypot(targetX - x, targetY - y);
    }

    public double x() { return x; }
    public double y() { return y; }
    public double targetX() { return targetX; }
    public double targetY() { return targetY; }
    public Facing facing() { return facing; }
    public boolean moving() { return moving; }
    public boolean alive() { return alive; }
}
