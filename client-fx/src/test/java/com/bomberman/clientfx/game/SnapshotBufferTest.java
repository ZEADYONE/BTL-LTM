package com.bomberman.clientfx.game;

import com.bomberman.common.dto.GameStateDto;
import com.bomberman.common.enums.GameStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SnapshotBufferTest {

    @Test
    void showsTheSnapshotFromOneHundredMillisAgo() {
        SnapshotBuffer buffer = new SnapshotBuffer();
        for (long tick = 1; tick <= 6; tick++) {
            buffer.add(snapshot(tick), ms(tick * 50));
        }

        assertEquals(4, buffer.sample(ms(300)).tick());
    }

    @Test
    void bunchedArrivalsAfterLagStillPlayBackOneTickAtATime() {
        SnapshotBuffer buffer = new SnapshotBuffer();
        buffer.add(snapshot(1), ms(50));
        buffer.add(snapshot(2), ms(100));
        // Ticks 3-6 were held up by the network and arrive together.
        for (long tick = 3; tick <= 6; tick++) {
            buffer.add(snapshot(tick), ms(330));
        }

        assertEquals(4, buffer.sample(ms(310)).tick());
        assertEquals(5, buffer.sample(ms(360)).tick());
        assertEquals(6, buffer.sample(ms(410)).tick());
    }

    @Test
    void holdsTheNewestSnapshotWhenNothingNewerArrives() {
        SnapshotBuffer buffer = new SnapshotBuffer();
        buffer.add(snapshot(1), ms(50));
        buffer.add(snapshot(2), ms(100));

        assertEquals(2, buffer.sample(ms(1_000)).tick());
    }

    @Test
    void newMatchWithLowerTicksStartsOver() {
        SnapshotBuffer buffer = new SnapshotBuffer();
        buffer.add(snapshot(500), ms(25_000));
        buffer.add(snapshot(1), ms(26_000));

        assertEquals(1, buffer.sample(ms(26_000)).tick());
    }

    @Test
    void emptyBufferHasNoSample() {
        assertNull(new SnapshotBuffer().sample(ms(0)));
    }

    private static GameStateDto snapshot(long tick) {
        return new GameStateDto(tick, GameStatus.RUNNING, null, List.of(), List.of(), List.of(), 0);
    }

    private static long ms(long millis) {
        return millis * 1_000_000L;
    }
}
