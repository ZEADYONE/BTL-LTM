package com.bomberman.server.room;

import com.bomberman.common.enums.RoomStatus;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * In-memory room aggregate. Mutation is coordinated by {@link RoomManager}.
 */
public final class GameRoom {

    public static final int MAX_PLAYERS = 4;

    private final String roomId;
    private final String roomName;
    private final Map<Long, RoomPlayer> players = new LinkedHashMap<>();
    private final Set<Long> kickedUserIds = new HashSet<>();
    private long hostUserId;
    private RoomStatus status = RoomStatus.WAITING;

    GameRoom(String roomId, String roomName, RoomPlayer host) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.hostUserId = host.getUserId();
        players.put(host.getUserId(), host);
    }

    void addPlayer(RoomPlayer player) {
        players.put(player.getUserId(), player);
    }

    void removePlayer(long userId) {
        players.remove(userId);
        if (!players.isEmpty() && hostUserId == userId) {
            hostUserId = players.keySet().iterator().next();
        }
    }

    void kickPlayer(long userId) {
        removePlayer(userId);
        kickedUserIds.add(userId);
    }

    boolean wasKicked(long userId) {
        return kickedUserIds.contains(userId);
    }

    void allowKickedPlayer(long userId) {
        kickedUserIds.remove(userId);
    }

    RoomPlayer getPlayer(long userId) {
        return players.get(userId);
    }

    boolean isFull() {
        return players.size() >= MAX_PLAYERS;
    }

    boolean isEmpty() {
        return players.isEmpty();
    }

    boolean allPlayersReady() {
        return players.values().stream().allMatch(RoomPlayer::isReady);
    }

    void start() {
        status = RoomStatus.PLAYING;
    }

    void finish() {
        status = RoomStatus.FINISHED;
    }

    void prepareRematch() {
        status = RoomStatus.WAITING;
        players.values().forEach(player -> player.setReady(false));
    }

    String getRoomId() {
        return roomId;
    }

    String getRoomName() {
        return roomName;
    }

    long getHostUserId() {
        return hostUserId;
    }

    Map<Long, RoomPlayer> getPlayers() {
        return players;
    }

    RoomStatus getStatus() {
        return status;
    }
}
