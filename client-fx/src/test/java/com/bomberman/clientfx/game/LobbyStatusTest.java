package com.bomberman.clientfx.game;

import com.bomberman.common.dto.RoomPlayerDto;
import com.bomberman.common.dto.RoomStateDto;
import com.bomberman.common.enums.RoomStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LobbyStatusTest {

    private static final long HOST = 1;
    private static final long GUEST = 2;

    @Test
    void aloneTheHostCannotStart() {
        LobbyStatus status = LobbyStatus.of(room(new RoomPlayerDto(HOST, "host", true)), HOST);

        assertFalse(status.enoughPlayers());
        assertTrue(status.everyoneReady());
        assertFalse(status.canStart());
        assertEquals("Waiting for players…", status.statusLine());
    }

    @Test
    void theHostMustBeReadyToo() {
        LobbyStatus status = LobbyStatus.of(room(
                new RoomPlayerDto(HOST, "host", false),
                new RoomPlayerDto(GUEST, "guest", true)), HOST);

        assertTrue(status.enoughPlayers());
        assertFalse(status.everyoneReady());
        assertEquals(1, status.readyPlayers());
        assertFalse(status.canStart());
    }

    @Test
    void onlyTheHostSeesStartOnceEveryoneIsReady() {
        RoomStateDto room = room(new RoomPlayerDto(HOST, "host", true), new RoomPlayerDto(GUEST, "guest", true));

        LobbyStatus host = LobbyStatus.of(room, HOST);
        LobbyStatus guest = LobbyStatus.of(room, GUEST);

        assertTrue(host.canStart());
        assertEquals("Ready to start!", host.statusLine());
        assertFalse(guest.canStart());
        assertEquals("Waiting for host to start…", guest.statusLine());
        assertTrue(guest.youReady());
    }

    private static RoomStateDto room(RoomPlayerDto... players) {
        return new RoomStateDto("room", "Room", HOST, List.of(players), 4, RoomStatus.WAITING);
    }
}
