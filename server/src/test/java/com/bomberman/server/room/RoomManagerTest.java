package com.bomberman.server.room;

import com.bomberman.common.enums.RoomResultCode;
import com.bomberman.common.enums.RoomStatus;
import com.bomberman.server.auth.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

class RoomManagerTest {

    private RoomManager roomManager;

    @BeforeEach
    void setUp() {
        roomManager = new RoomManager();
    }

    @Test
    void createsRoomWithCreatorAsHost() {
        AuthenticatedUser host = user(1);

        RoomOperationResult result = roomManager.createRoom(host, session(1), "Room One");

        assertEquals(RoomResultCode.SUCCESS, result.result());
        assertNotNull(result.room().roomId());
        assertEquals("Room One", result.room().roomName());
        assertEquals(host.userId(), result.room().hostUserId());
        assertEquals(1, result.room().players().size());
        assertEquals(RoomStatus.WAITING, result.room().status());
    }

    @Test
    void playerCanJoinWaitingRoom() {
        RoomSnapshot room = createRoom(user(1));

        RoomOperationResult result = roomManager.joinRoom(user(2), session(2), room.roomId());

        assertEquals(RoomResultCode.SUCCESS, result.result());
        assertEquals(2, result.room().players().size());
    }

    @Test
    void fifthPlayerCannotJoinFullRoom() {
        RoomSnapshot room = createRoom(user(1));
        roomManager.joinRoom(user(2), session(2), room.roomId());
        roomManager.joinRoom(user(3), session(3), room.roomId());
        roomManager.joinRoom(user(4), session(4), room.roomId());

        RoomOperationResult result = roomManager.joinRoom(user(5), session(5), room.roomId());

        assertEquals(RoomResultCode.ROOM_FULL, result.result());
        assertEquals(4, roomManager.snapshotRooms().getFirst().players().size());
    }

    @Test
    void leavingLastPlayerDeletesRoom() {
        AuthenticatedUser host = user(1);
        createRoom(host);

        RoomOperationResult result = roomManager.leaveRoom(host.userId());

        assertEquals(RoomResultCode.SUCCESS, result.result());
        assertNull(result.room());
        assertTrue(roomManager.snapshotRooms().isEmpty());
        assertNull(roomManager.findRoomByUser(host.userId()));
    }

    @Test
    void leavingHostTransfersHostToNextPlayer() {
        AuthenticatedUser host = user(1);
        AuthenticatedUser nextPlayer = user(2);
        RoomSnapshot room = createRoom(host);
        roomManager.joinRoom(nextPlayer, session(2), room.roomId());

        RoomOperationResult result = roomManager.leaveRoom(host.userId());

        assertEquals(RoomResultCode.SUCCESS, result.result());
        assertEquals(nextPlayer.userId(), result.room().hostUserId());
    }

    @Test
    void playerCanChangeReadyState() {
        AuthenticatedUser host = user(1);
        createRoom(host);

        RoomOperationResult result = roomManager.setReady(host.userId(), true);

        assertEquals(RoomResultCode.SUCCESS, result.result());
        assertTrue(result.room().players().getFirst().ready());
    }

    @Test
    void nonHostCannotStartGame() {
        AuthenticatedUser host = user(1);
        AuthenticatedUser guest = user(2);
        RoomSnapshot room = createRoom(host);
        roomManager.joinRoom(guest, session(2), room.roomId());

        RoomOperationResult result = roomManager.startGame(guest.userId());

        assertEquals(RoomResultCode.NOT_HOST, result.result());
    }

    @Test
    void hostCannotStartUntilEveryPlayerIsReady() {
        AuthenticatedUser host = user(1);
        AuthenticatedUser guest = user(2);
        RoomSnapshot room = createRoom(host);
        roomManager.joinRoom(guest, session(2), room.roomId());
        roomManager.setReady(host.userId(), true);

        RoomOperationResult result = roomManager.startGame(host.userId());

        assertEquals(RoomResultCode.NOT_ALL_PLAYERS_READY, result.result());
    }

    @Test
    void startsGameWhenHostHasAtLeastTwoReadyPlayers() {
        AuthenticatedUser host = user(1);
        AuthenticatedUser guest = user(2);
        RoomSnapshot room = createRoom(host);
        roomManager.joinRoom(guest, session(2), room.roomId());
        roomManager.setReady(host.userId(), true);
        roomManager.setReady(guest.userId(), true);

        RoomOperationResult result = roomManager.startGame(host.userId());

        assertEquals(RoomResultCode.SUCCESS, result.result());
        assertEquals(RoomStatus.PLAYING, result.room().status());
    }

    @Test
    void concurrentJoinsNeverExceedRoomCapacity() throws Exception {
        RoomSnapshot room = createRoom(user(1));
        AtomicInteger successfulJoins = new AtomicInteger();

        try (var executor = Executors.newFixedThreadPool(8)) {
            for (long userId = 2; userId <= 12; userId++) {
                long joiningUserId = userId;
                executor.submit(() -> {
                    RoomOperationResult result = roomManager.joinRoom(
                            user(joiningUserId),
                            session(joiningUserId),
                            room.roomId()
                    );
                    if (result.isSuccess()) {
                        successfulJoins.incrementAndGet();
                    }
                });
            }
            executor.shutdown();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }

        assertEquals(3, successfulJoins.get());
        assertEquals(4, roomManager.snapshotRooms().getFirst().players().size());
    }

    @Test
    void rematchReturnsFinishedRoomToWaitingAndClearsReadyFlags() {
        AuthenticatedUser host = user(1);
        AuthenticatedUser guest = user(2);
        RoomSnapshot room = createRoom(host);
        roomManager.joinRoom(guest, session(2), room.roomId());
        roomManager.setReady(host.userId(), true);
        roomManager.setReady(guest.userId(), true);
        roomManager.startGame(host.userId());
        roomManager.finishGame(room.roomId());

        RoomOperationResult result = roomManager.prepareRematch(guest.userId());

        assertEquals(RoomStatus.WAITING, result.room().status());
        assertTrue(result.room().players().stream().noneMatch(RoomPlayerSnapshot::ready));
    }

    private RoomSnapshot createRoom(AuthenticatedUser host) {
        return roomManager.createRoom(host, session(host.userId()), "Test Room").room();
    }

    private AuthenticatedUser user(long id) {
        return new AuthenticatedUser(id, "user-" + id);
    }

    private String session(long id) {
        return "session-" + id;
    }
}
