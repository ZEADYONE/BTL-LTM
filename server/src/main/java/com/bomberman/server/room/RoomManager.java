package com.bomberman.server.room;

import com.bomberman.common.enums.RoomResultCode;
import com.bomberman.common.enums.RoomStatus;
import com.bomberman.server.auth.AuthenticatedUser;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Owns all in-memory rooms and atomically maintains the one-room-per-user rule.
 */
@Component
public class RoomManager {

    private static final int MAX_ROOM_NAME_LENGTH = 60;

    private final ConcurrentMap<String, GameRoom> rooms = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, String> roomByUser = new ConcurrentHashMap<>();
    private final Lock mutationLock = new ReentrantLock();

    public RoomOperationResult createRoom(
            AuthenticatedUser user,
            String sessionId,
            String rawRoomName
    ) {
        mutationLock.lock();
        try {
            String roomName = normalizeRoomName(rawRoomName);
            if (roomName == null || roomName.isBlank() || roomName.length() > MAX_ROOM_NAME_LENGTH) {
                return failure(RoomResultCode.INVALID_ROOM_NAME);
            }
            if (roomByUser.containsKey(user.userId())) {
                return failure(RoomResultCode.ALREADY_IN_ROOM);
            }

            String roomId = UUID.randomUUID().toString();
            GameRoom room = new GameRoom(
                    roomId,
                    roomName,
                    new RoomPlayer(user.userId(), user.username(), sessionId)
            );
            rooms.put(roomId, room);
            roomByUser.put(user.userId(), roomId);
            return success(room);
        } finally {
            mutationLock.unlock();
        }
    }

    public RoomOperationResult joinRoom(
            AuthenticatedUser user,
            String sessionId,
            String roomId
    ) {
        mutationLock.lock();
        try {
            if (roomByUser.containsKey(user.userId())) {
                return failure(RoomResultCode.ALREADY_IN_ROOM);
            }
            GameRoom room = rooms.get(roomId);
            if (room == null) {
                return failure(RoomResultCode.ROOM_NOT_FOUND);
            }
            if (room.getStatus() != RoomStatus.WAITING) {
                return failure(RoomResultCode.ROOM_NOT_WAITING);
            }
            if (room.isFull()) {
                return failure(RoomResultCode.ROOM_FULL);
            }

            room.addPlayer(new RoomPlayer(user.userId(), user.username(), sessionId));
            roomByUser.put(user.userId(), roomId);
            return success(room);
        } finally {
            mutationLock.unlock();
        }
    }

    public RoomOperationResult leaveRoom(long userId) {
        mutationLock.lock();
        try {
            String roomId = roomByUser.remove(userId);
            if (roomId == null) {
                return failure(RoomResultCode.NOT_IN_ROOM);
            }

            GameRoom room = rooms.get(roomId);
            if (room == null) {
                return failure(RoomResultCode.ROOM_NOT_FOUND);
            }
            room.removePlayer(userId);
            if (room.isEmpty()) {
                rooms.remove(roomId);
                return new RoomOperationResult(RoomResultCode.SUCCESS, null);
            }
            return success(room);
        } finally {
            mutationLock.unlock();
        }
    }

    public RoomOperationResult setReady(long userId, boolean ready) {
        mutationLock.lock();
        try {
            GameRoom room = findRoomForUser(userId);
            if (room == null) {
                return failure(RoomResultCode.NOT_IN_ROOM);
            }
            if (room.getStatus() != RoomStatus.WAITING) {
                return failure(RoomResultCode.ROOM_NOT_WAITING);
            }
            room.getPlayer(userId).setReady(ready);
            return success(room);
        } finally {
            mutationLock.unlock();
        }
    }

    public RoomOperationResult startGame(long userId) {
        mutationLock.lock();
        try {
            GameRoom room = findRoomForUser(userId);
            if (room == null) {
                return failure(RoomResultCode.NOT_IN_ROOM);
            }
            if (room.getStatus() != RoomStatus.WAITING) {
                return failure(RoomResultCode.ROOM_NOT_WAITING);
            }
            if (room.getHostUserId() != userId) {
                return failure(RoomResultCode.NOT_HOST);
            }
            if (room.getPlayers().size() < 2) {
                return failure(RoomResultCode.NOT_ENOUGH_PLAYERS);
            }
            if (!room.allPlayersReady()) {
                return failure(RoomResultCode.NOT_ALL_PLAYERS_READY);
            }

            room.start();
            return success(room);
        } finally {
            mutationLock.unlock();
        }
    }

    public RoomOperationResult finishGame(String roomId) {
        mutationLock.lock();
        try {
            GameRoom room = rooms.get(roomId);
            if (room == null) {
                return failure(RoomResultCode.ROOM_NOT_FOUND);
            }
            room.finish();
            return success(room);
        } finally {
            mutationLock.unlock();
        }
    }

    public RoomOperationResult prepareRematch(long userId) {
        mutationLock.lock();
        try {
            GameRoom room = findRoomForUser(userId);
            if (room == null) {
                return failure(RoomResultCode.NOT_IN_ROOM);
            }
            if (room.getStatus() != RoomStatus.FINISHED) {
                return failure(RoomResultCode.ROOM_NOT_FINISHED);
            }
            room.prepareRematch();
            return success(room);
        } finally {
            mutationLock.unlock();
        }
    }

    public List<RoomSnapshot> snapshotRooms() {
        mutationLock.lock();
        try {
            return rooms.values().stream()
                    .map(this::snapshot)
                    .sorted(Comparator.comparing(RoomSnapshot::roomName))
                    .toList();
        } finally {
            mutationLock.unlock();
        }
    }

    public RoomSnapshot findRoomByUser(long userId) {
        mutationLock.lock();
        try {
            GameRoom room = findRoomForUser(userId);
            return room == null ? null : snapshot(room);
        } finally {
            mutationLock.unlock();
        }
    }

    private GameRoom findRoomForUser(long userId) {
        String roomId = roomByUser.get(userId);
        return roomId == null ? null : rooms.get(roomId);
    }

    private RoomOperationResult success(GameRoom room) {
        return new RoomOperationResult(RoomResultCode.SUCCESS, snapshot(room));
    }

    private RoomOperationResult failure(RoomResultCode result) {
        return new RoomOperationResult(result, null);
    }

    private RoomSnapshot snapshot(GameRoom room) {
        List<RoomPlayerSnapshot> players = room.getPlayers().values().stream()
                .map(player -> new RoomPlayerSnapshot(
                        player.getUserId(),
                        player.getUsername(),
                        player.getSessionId(),
                        player.isReady()
                ))
                .toList();
        return new RoomSnapshot(
                room.getRoomId(),
                room.getRoomName(),
                room.getHostUserId(),
                players,
                GameRoom.MAX_PLAYERS,
                room.getStatus()
        );
    }

    private String normalizeRoomName(String roomName) {
        return roomName == null ? null : roomName.trim();
    }
}
