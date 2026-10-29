package com.bomberman.clientfx.game;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GameFormatsTest {

    @Test
    void formatsHalfPointUnits() {
        assertEquals("0.0", GameFormats.points(0));
        assertEquals("0.5", GameFormats.points(1));
        assertEquals("1.0", GameFormats.points(2));
        assertEquals("+0.5", GameFormats.earnedPoints(1));
    }

    @Test
    void formatsDurationAndLocalTime() {
        assertEquals("03:42", GameFormats.duration(1_000, 223_000));
        long timestamp = Instant.parse("2026-09-26T13:15:00Z").toEpochMilli();
        assertEquals("2026-09-26 20:15", GameFormats.localTime(timestamp, ZoneId.of("Asia/Ho_Chi_Minh")));
    }

    @Test
    void clampsNegativeDuration() {
        assertEquals("00:00", GameFormats.duration(2_000, 1_000));
    }
}
