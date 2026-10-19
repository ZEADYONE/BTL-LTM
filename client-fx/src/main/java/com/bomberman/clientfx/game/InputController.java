package com.bomberman.clientfx.game;

import com.bomberman.common.enums.Direction;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Implements held-direction priority and repeat timing independently from JavaFX key events.
 * At most one MOVE goes out per {@link #MOVE_REPEAT_NANOS}, matching the server's walking pace:
 * a press inside that window is remembered and sent when it ends instead of being sent early.
 */
public final class InputController {

    public static final long MOVE_REPEAT_NANOS = 140_000_000L;

    private final Consumer<Direction> move;
    private final Runnable placeBomb;
    private final BooleanSupplier inputAllowed;
    private final Deque<Direction> heldDirections = new ArrayDeque<>();
    private boolean bombHeld;
    private long nextRepeatNanos = Long.MAX_VALUE;
    private long earliestMoveNanos = Long.MIN_VALUE;
    /** Pressed inside the repeat window; sent when it ends even if the key was released by then. */
    private Direction queuedPress;

    public InputController(Consumer<Direction> move, Runnable placeBomb, BooleanSupplier inputAllowed) {
        this.move = Objects.requireNonNull(move, "move must not be null");
        this.placeBomb = Objects.requireNonNull(placeBomb, "placeBomb must not be null");
        this.inputAllowed = Objects.requireNonNull(inputAllowed, "inputAllowed must not be null");
    }

    public void pressDirection(Direction direction, long nowNanos) {
        Objects.requireNonNull(direction, "direction must not be null");
        heldDirections.remove(direction);
        heldDirections.addFirst(direction);
        if (nowNanos < earliestMoveNanos) {
            queuedPress = direction;
        }
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
        if (!inputAllowed.getAsBoolean()) {
            queuedPress = null;
            if (!heldDirections.isEmpty()) {
                nextRepeatNanos = nowNanos + MOVE_REPEAT_NANOS;
            }
            return;
        }
        if (!heldDirections.isEmpty()) {
            if (nowNanos >= nextRepeatNanos) {
                emitMove(nowNanos);
            }
        } else if (queuedPress != null && nowNanos >= earliestMoveNanos) {
            emitMove(nowNanos);
        }
    }

    /** Clears OS key state after focus loss or leaving the game screen. */
    public void reset() {
        heldDirections.clear();
        bombHeld = false;
        nextRepeatNanos = Long.MAX_VALUE;
        earliestMoveNanos = Long.MIN_VALUE;
        queuedPress = null;
    }

    /** Sends now if the last move is old enough, otherwise waits for the repeat window to end. */
    private void sendMoveNow(long nowNanos) {
        if (nowNanos < earliestMoveNanos) {
            nextRepeatNanos = earliestMoveNanos;
            return;
        }
        if (inputAllowed.getAsBoolean()) {
            emitMove(nowNanos);
        } else {
            nextRepeatNanos = nowNanos + MOVE_REPEAT_NANOS;
        }
    }

    private void emitMove(long nowNanos) {
        Direction direction = queuedPress != null ? queuedPress : heldDirections.getFirst();
        queuedPress = null;
        move.accept(direction);
        nextRepeatNanos = nowNanos + MOVE_REPEAT_NANOS;
        earliestMoveNanos = nextRepeatNanos;
    }
}
