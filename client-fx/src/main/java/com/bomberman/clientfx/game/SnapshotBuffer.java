package com.bomberman.clientfx.game;

import com.bomberman.clientfx.ServerRules;
import com.bomberman.common.dto.GameStateDto;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * Jitter buffer for the other players: they are shown as of {@link #DELAY_NANOS} ago on the
 * server's tick timeline, so snapshots that arrive late or bunched together still play back at
 * an even pace. The tick timeline is mapped to the local clock from the fastest arrival seen
 * (server ticks are a fixed {@link ServerRules#TICK_MILLIS} apart). Used on the UI thread only.
 */
public final class SnapshotBuffer {

    public static final long DELAY_NANOS = 100_000_000L;
    private static final long TICK_NANOS = ServerRules.TICK_MILLIS * 1_000_000L;
    /** Lets the clock mapping drift later by up to 5 ms/s, so a lasting rise in latency is absorbed. */
    private static final long OFFSET_RELAX_NANOS = 250_000L;
    private static final int CAPACITY = 64;

    private final Deque<GameStateDto> snapshots = new ArrayDeque<>();
    /** Local time of server tick 0 plus the smallest latency seen. */
    private long offsetNanos;

    public void reset() {
        snapshots.clear();
    }

    /** @param receivedNanos arrival time on the {@link System#nanoTime()} clock */
    public void add(GameStateDto snapshot, long receivedNanos) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        GameStateDto last = snapshots.peekLast();
        if (last != null && snapshot.tick() <= last.tick()) {
            if (snapshot.tick() == last.tick()) {
                return;
            }
            reset(); // Ticks restart with each match.
        }
        long sample = receivedNanos - snapshot.tick() * TICK_NANOS;
        if (snapshots.isEmpty() || sample < offsetNanos) {
            offsetNanos = sample;
        } else {
            offsetNanos += Math.min(sample - offsetNanos, OFFSET_RELAX_NANOS);
        }
        snapshots.addLast(snapshot);
        while (snapshots.size() > CAPACITY) {
            snapshots.removeFirst();
        }
    }

    /**
     * The newest snapshot at or before the delayed render time, or the oldest one when all are
     * newer; null when empty. Older snapshots are discarded.
     */
    public GameStateDto sample(long nowNanos) {
        if (snapshots.isEmpty()) {
            return null;
        }
        long renderTick = Math.floorDiv(nowNanos - DELAY_NANOS - offsetNanos, TICK_NANOS);
        while (snapshots.size() > 1) {
            GameStateDto first = snapshots.pollFirst();
            if (snapshots.peekFirst().tick() > renderTick) {
                snapshots.addFirst(first);
                break;
            }
        }
        return snapshots.peekFirst();
    }
}
