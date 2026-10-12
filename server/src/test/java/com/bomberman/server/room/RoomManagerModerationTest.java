package com.bomberman.server.room;

import com.bomberman.common.enums.RoomResultCode;
import com.bomberman.common.enums.RoomStatus;
import com.bomberman.server.auth.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoomManagerModerationTest {

    private RoomManager roomManager;

    @BeforeEach
    void setUp() {
        roomManager = new RoomManager();
    }

    @Test
    void hostKicksGuestAndDirectRejoinRemainsBlocked() {
        RoomSnapshot room = createRoom(1);
        roomManager.joinRoom(user(2), session(2), room.roomId());

        RoomMemberRemovalResult result = roomManager.kickMember(1, 2);

        assertEquals(RoomResultCode.SUCCESS, result.result());
        assertEquals(2, result.removedPlayer().userId());
        assertEquals(session(2), result.removedPlayer().sessionId());
        assertEquals(1, result.room().players().size());
        assertNull(roomManager.findRoomByUser(2));
        assertEquals(
                RoomResultCode.KICKED_FROM_ROOM,
                roomManager.joinRoom(user(2), session(2), room.roomId()).result()
        );
    }

    @Test
    void kickValidatesHostSelfTargetAndRoomStatus() {
        RoomSnapshot room = createRoom(1);
        roomManager.joinRoom(user(2), session(2), room.roomId());

        assertEquals(RoomResultCode.NOT_HOST, roomManager.kickMember(2, 1).result());
        assertEquals(RoomResultCode.CANNOT_KICK_SELF, roomManager.kickMember(1, 1).result());
        assertEquals(RoomResultCode.TARGET_NOT_IN_ROOM, roomManager.kickMember(1, 99).result());

        roomManager.setReady(1, true);
        roomManager.setReady(2, true);
        roomManager.startGame(1);
        assertEquals(RoomResultCode.ROOM_NOT_WAITING, roomManager.kickMember(1, 2).result());
    }

    @Test
    void concurrentKickAndLeaveNeverLeaveStaleMembership() throws Exception {
        RoomSnapshot room = createRoom(1);
        roomManager.joinRoom(user(2), session(2), room.roomId());
        CyclicBarrier start = new CyclicBarrier(2);

        try (var executor = Executors.newFixedThreadPool(2)) {
            Future<RoomMemberRemovalResult> kick = executor.submit(() -> {
                start.await();
                return roomManager.kickMember(1, 2);
            });
            Future<RoomOperationResult> leave = executor.submit(() -> {
                start.await();
                return roomManager.leaveRoom(2);
            });

            RoomResultCode kickCode = kick.get().result();
            RoomResultCode leaveCode = leave.get().result();
            assertTrue(Set.of(RoomResultCode.SUCCESS, RoomResultCode.TARGET_NOT_IN_ROOM).contains(kickCode));
            assertTrue(Set.of(RoomResultCode.SUCCESS, RoomResultCode.NOT_IN_ROOM).contains(leaveCode));
            assertTrue(kickCode == RoomResultCode.SUCCESS || leaveCode == RoomResultCode.SUCCESS);
            assertNull(roomManager.findRoomByUser(2));

            RoomResultCode rejoin = roomManager.joinRoom(user(2), session(2), room.roomId()).result();
            assertEquals(
                    kickCode == RoomResultCode.SUCCESS
                            ? RoomResultCode.KICKED_FROM_ROOM
                            : RoomResultCode.SUCCESS,
                    rejoin
            );
        }
    }

    @Test
    void concurrentKickAndStartProduceOneConsistentRoom() throws Exception {
        RoomSnapshot room = createRoom(1);
        roomManager.joinRoom(user(2), session(2), room.roomId());
        roomManager.joinRoom(user(3), session(3), room.roomId());
        roomManager.setReady(1, true);
        roomManager.setReady(2, true);
        roomManager.setReady(3, true);
        CyclicBarrier start = new CyclicBarrier(2);

        try (var executor = Executors.newFixedThreadPool(2)) {
            Future<RoomMemberRemovalResult> kick = executor.submit(() -> {
                start.await();
                return roomManager.kickMember(1, 2);
            });
            Future<RoomOperationResult> gameStart = executor.submit(() -> {
                start.await();
                return roomManager.startGame(1);
            });

            RoomResultCode kickCode = kick.get().result();
            assertEquals(RoomResultCode.SUCCESS, gameStart.get().result());
            assertTrue(Set.of(RoomResultCode.SUCCESS, RoomResultCode.ROOM_NOT_WAITING).contains(kickCode));
            RoomSnapshot finalRoom = roomManager.findRoomByUser(1);
            assertEquals(RoomStatus.PLAYING, finalRoom.status());
            assertEquals(kickCode == RoomResultCode.SUCCESS ? 2 : 3, finalRoom.players().size());
        }
    }

    @Test
    void onlyCurrentHostInvitationAllowsKickedPlayerBack() {
        RoomSnapshot room = createRoom(1);
        roomManager.joinRoom(user(2), session(2), room.roomId());
        roomManager.joinRoom(user(3), session(3), room.roomId());
        roomManager.kickMember(1, 2);

        assertEquals(RoomResultCode.KICKED_FROM_ROOM,
                roomManager.joinRoomByInvite(user(2), session(2), room.roomId(), 3).result());
        assertEquals(RoomResultCode.SUCCESS,
                roomManager.joinRoomByInvite(user(2), session(2), room.roomId(), 1).result());
        assertEquals(3, roomManager.findRoomByUser(1).players().size());
    }

    @Test
    void invitationCannotBeUsedAfterInviterLeaves() {
        RoomSnapshot room = createRoom(1);
        roomManager.joinRoom(user(2), session(2), room.roomId());
        roomManager.leaveRoom(2);

        assertEquals(RoomResultCode.TARGET_NOT_IN_ROOM,
                roomManager.joinRoomByInvite(user(3), session(3), room.roomId(), 2).result());
    }

    private RoomSnapshot createRoom(long hostId) {
        return roomManager.createRoom(user(hostId), session(hostId), "Moderated room").room();
    }

    private static AuthenticatedUser user(long id) {
        return new AuthenticatedUser(id, "user-" + id);
    }

    private static String session(long id) {
        return "session-" + id;
    }
}
