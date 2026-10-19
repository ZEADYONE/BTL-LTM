package com.bomberman.server.game;

import com.bomberman.common.enums.Direction;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Objects;

/** Authoritative mutable state of one player in a game. */
public final class BomberPlayer {

    public static final int DEFAULT_BOMB_CAPACITY = 1;
    public static final int DEFAULT_BOMB_RANGE = 2;
    /** Average walking pace, one tile per interval; matches the client's held-key repeat. */
    public static final Duration STEP_INTERVAL = Duration.ofMillis(140);
    /**
     * How early a step may be taken. One banked step absorbs network jitter; anything faster
     * (key spamming, a burst of MOVEs delivered together after lag) waits in the input queue.
     */
    static final Duration STEP_BURST_TOLERANCE = STEP_INTERVAL;
    /** Steps waiting for the pace; when a burst overflows it the oldest step is dropped. */
    public static final int MAX_QUEUED_STEPS = 3;
    /** Bombs and steps together, so spamming PLACE_BOMB cannot grow the queue without bound. */
    static final int MAX_QUEUED_INPUTS = 6;

    /** Input received from the client, applied by the game loop in arrival order. */
    sealed interface QueuedInput {
        record Step(Direction direction, long seq) implements QueuedInput {
        }

        record PlaceBomb() implements QueuedInput {
        }
    }

    private final long userId;
    private final String username;
    private Position position;
    private boolean alive = true;
    private int bombCapacity = DEFAULT_BOMB_CAPACITY;
    private int activeBombs;
    private int bombRange = DEFAULT_BOMB_RANGE;
    /** Pacing state (GCRA): when the next step is due at the average pace; null before the first step. */
    private Instant nextStepDue;
    private Instant lastStepAt;
    private final Deque<QueuedInput> queuedInputs = new ArrayDeque<>();
    private long lastMoveSeq;

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

    /** At most one step per game tick, and on average one per {@link #STEP_INTERVAL}. */
    boolean canStepAt(Instant now) {
        if (now.equals(lastStepAt)) {
            return false;
        }
        return nextStepDue == null || !now.isBefore(nextStepDue.minus(STEP_BURST_TOLERANCE));
    }

    void recordStep(Instant now) {
        Instant base = nextStepDue == null || nextStepDue.isBefore(now) ? now : nextStepDue;
        nextStepDue = base.plus(STEP_INTERVAL);
        lastStepAt = now;
    }

    /** Highest move sequence number applied, rejected or dropped; sent back so the client can reconcile. */
    public long getLastMoveSeq() {
        return lastMoveSeq;
    }

    void queueStep(Direction direction, long seq) {
        long queuedSteps = queuedInputs.stream().filter(QueuedInput.Step.class::isInstance).count();
        if (queuedSteps >= MAX_QUEUED_STEPS) {
            dropOldest(QueuedInput.Step.class);
        }
        enqueue(new QueuedInput.Step(Objects.requireNonNull(direction), seq));
    }

    void queueBomb() {
        enqueue(new QueuedInput.PlaceBomb());
    }

    QueuedInput peekQueuedInput() {
        return queuedInputs.peekFirst();
    }

    void pollQueuedInput() {
        queuedInputs.pollFirst();
    }

    /** Dead players and finished games do nothing with queued input, but its steps still count as handled. */
    void discardQueuedInputs() {
        queuedInputs.forEach(this::acknowledgeIfStep);
        queuedInputs.clear();
    }

    void acknowledgeMove(long seq) {
        lastMoveSeq = Math.max(lastMoveSeq, seq);
    }

    private void enqueue(QueuedInput input) {
        if (queuedInputs.size() >= MAX_QUEUED_INPUTS) {
            dropOldest(QueuedInput.class);
        }
        queuedInputs.addLast(input);
    }

    private void dropOldest(Class<? extends QueuedInput> type) {
        Iterator<QueuedInput> iterator = queuedInputs.iterator();
        while (iterator.hasNext()) {
            QueuedInput input = iterator.next();
            if (type.isInstance(input)) {
                iterator.remove();
                acknowledgeIfStep(input);
                return;
            }
        }
    }

    private void acknowledgeIfStep(QueuedInput input) {
        if (input instanceof QueuedInput.Step step) {
            acknowledgeMove(step.seq());
        }
    }
}
