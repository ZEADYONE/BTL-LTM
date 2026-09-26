package com.bomberman.clientfx.game;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Shared formatting for half-point scores, match duration and local timestamps. */
public final class GameFormats {

    private static final DateTimeFormatter MATCH_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private GameFormats() {
    }

    public static String points(long scoreUnits) {
        return String.format(Locale.ROOT, "%.1f", scoreUnits / 2.0);
    }

    public static String earnedPoints(int scoreUnits) {
        return "+" + points(scoreUnits);
    }

    public static String duration(long startedAtEpochMillis, long endedAtEpochMillis) {
        long seconds = Math.max(0, (endedAtEpochMillis - startedAtEpochMillis) / 1_000);
        return "%02d:%02d".formatted(seconds / 60, seconds % 60);
    }

    public static String localTime(long epochMillis, ZoneId zoneId) {
        return MATCH_TIME.withZone(zoneId).format(Instant.ofEpochMilli(epochMillis));
    }
}
