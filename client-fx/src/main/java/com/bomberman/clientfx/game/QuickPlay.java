package com.bomberman.clientfx.game;

import com.bomberman.clientfx.ServerRules;
import com.bomberman.common.dto.RoomSummaryDto;
import com.bomberman.common.enums.RoomStatus;

import java.util.Comparator;
import java.util.List;

/** Room choice for the Home PLAY button (docs/ui-redesign/02, section 3.2). */
public final class QuickPlay {

    /** Rooms tried before Quick Play gives up joining and creates a room instead. */
    public static final int MAX_JOIN_ATTEMPTS = 2;

    private static final String ROOM_SUFFIX = "'s Room";

    private QuickPlay() {
    }

    /**
     * Joinable rooms, fullest first; ties keep the server's order (sorted by name).
     */
    public static List<RoomSummaryDto> candidates(List<RoomSummaryDto> rooms) {
        return rooms.stream()
                .filter(room -> room.status() == RoomStatus.WAITING && room.playerCount() < room.maxPlayers())
                .sorted(Comparator.comparingInt(RoomSummaryDto::playerCount).reversed())
                .toList();
    }

    /** {@code "<username>'s Room"}, shortening the name so the result fits the server limit. */
    public static String defaultRoomName(String username) {
        int room = ServerRules.MAX_ROOM_NAME_LENGTH - ROOM_SUFFIX.length();
        String owner = username.strip();
        if (owner.length() > room) {
            owner = owner.substring(0, room).strip();
        }
        return owner + ROOM_SUFFIX;
    }

    /** Whether a failed join should move on to the next candidate rather than stop. */
    public static boolean isRetryable(String errorCode) {
        return "ROOM_FULL".equals(errorCode)
                || "ROOM_NOT_WAITING".equals(errorCode)
                || "ROOM_NOT_FOUND".equals(errorCode);
    }
}
