package com.bomberman.clientfx.game;

import com.bomberman.common.dto.RoomSummaryDto;
import com.bomberman.common.enums.RoomStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuickPlayTest {

    @Test
    void picksJoinableRoomsFullestFirstKeepingServerOrderForTies() {
        List<RoomSummaryDto> rooms = List.of(
                room("a", 1, RoomStatus.WAITING),
                room("b", 3, RoomStatus.WAITING),
                room("c", 4, RoomStatus.WAITING),
                room("d", 2, RoomStatus.PLAYING),
                room("e", 3, RoomStatus.WAITING),
                room("f", 1, RoomStatus.FINISHED)
        );

        assertEquals(List.of("b", "e", "a"), QuickPlay.candidates(rooms).stream().map(RoomSummaryDto::roomId).toList());
    }

    @Test
    void noJoinableRoomMeansCreatingOne() {
        assertTrue(QuickPlay.candidates(List.of(room("x", 4, RoomStatus.WAITING))).isEmpty());
        assertTrue(QuickPlay.candidates(List.of()).isEmpty());
    }

    @Test
    void defaultRoomNameFitsTheServerLimit() {
        assertEquals("Minh Đức's Room", QuickPlay.defaultRoomName("Minh Đức"));
        assertEquals(57, QuickPlay.defaultRoomName("x".repeat(50)).length(), "longest valid username fits untouched");
        String trimmed = QuickPlay.defaultRoomName("x".repeat(70));
        assertEquals(60, trimmed.length());
        assertTrue(trimmed.endsWith("'s Room"));
    }

    @Test
    void onlyRaceConditionsAreRetried() {
        assertTrue(QuickPlay.isRetryable("ROOM_FULL"));
        assertTrue(QuickPlay.isRetryable("ROOM_NOT_WAITING"));
        assertTrue(QuickPlay.isRetryable("ROOM_NOT_FOUND"));
        assertFalse(QuickPlay.isRetryable("ALREADY_IN_ROOM"));
        assertFalse(QuickPlay.isRetryable("NOT_AUTHENTICATED"));
    }

    private static RoomSummaryDto room(String id, int players, RoomStatus status) {
        return new RoomSummaryDto(id, "Room " + id, 1L, players, 4, status);
    }
}
