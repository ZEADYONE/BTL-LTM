package com.bomberman.clientfx.game;

import com.bomberman.common.enums.Direction;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Implements held-direction priority and repeat timing independently from JavaFX key events. */
public final class InputController {

    public static final long MOVE_REPEAT_NANOS = 140_000_000L;

    private final Consumer<Direction> move;
    private final Runnable placeBomb;
    private final BooleanSupplier inputAllowed;
    private final Deque<Direction> heldDirections = new ArrayDeque<>();
    private boolean bombHeld;
    private long nextRepeatNanos = Long.MAX_VALUE;

    public InputController(Consumer<Direction> move, Runnable placeBomb, BooleanSupplier inputAllowed) {
        this.move = Objects.requireNonNull(move, "move must not be null");
        this.placeBomb = Objects.requireNonNull(placeBomb, "placeBomb must not be null");
        this.inputAllowed = Objects.requireNonNull(inputAllowed, "inputAllowed must not be null");
    }

    public void pressDirection(Direction direction, long nowNanos) {
        Objects.requireNonNull(direction, "direction must not be null");
        heldDirections.remove(direction);
        heldDirections.addFirst(direction);
        sendMoveNow(nowNanos);
    }

    public void releaseDirection(Direction direction, long nowNanos) {
        Objects.requireNonNull(direction, "direction must not be null");
        boolean wasActive = direction == heldDirections.peekFirst();
        heldDirections.remove(direction);
        if (wasActive && !heldDirections.isEmpty()) {
            sendMoveNow(nowNanos);
        } else if (heldDirections.isEmpty()) {
            nextRepeatNanos = Long.MAX_VALUE;
        }
    }

    public void pressBomb() {
        if (!bombHeld) {
            bombHeld = true;
            if (inputAllowed.getAsBoolean()) {
                placeBomb.run();
            }
        }
    }

    public void releaseBomb() {
        bombHeld = false;
    }

    /** Called by the render loop. At most one MOVE is emitted per frame. */
    public void update(long nowNanos) {
        if (heldDirections.isEmpty()) {
            return;
        }
        if (!inputAllowed.getAsBoolean()) {
            nextRepeatNanos = nowNanos + MOVE_REPEAT_NANOS;
            return;
        }
        if (nowNanos >= nextRepeatNanos) {
            move.accept(heldDirections.getFirst());
            nextRepeatNanos = nowNanos + MOVE_REPEAT_NANOS;
        }
    }

    /** Clears OS key state after focus loss or leaving the game screen. */
    public void reset() {
        heldDirections.clear();
        bombHeld = false;
        nextRepeatNanos = Long.MAX_VALUE;
    }

    private void sendMoveNow(long nowNanos) {
        if (inputAllowed.getAsBoolean()) {
            move.accept(heldDirections.getFirst());
        }
        nextRepeatNanos = nowNanos + MOVE_REPEAT_NANOS;
    }
}
