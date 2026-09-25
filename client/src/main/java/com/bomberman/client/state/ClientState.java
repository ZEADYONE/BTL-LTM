package com.bomberman.client.state;

import com.bomberman.common.dto.GameStateDto;
import com.bomberman.common.dto.GameOverDto;
import com.bomberman.common.enums.GameStatus;
import com.bomberman.common.dto.OnlineUserDto;
import com.bomberman.common.dto.RoomStateDto;
import com.bomberman.common.dto.RoomSummaryDto;
import com.bomberman.common.dto.RankingEntryDto;
import com.bomberman.common.dto.MatchHistoryEntryDto;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/** Mutable presentation state; updates are applied on the libGDX application thread. */
public final class ClientState {

    private final List<ClientStateListener> listeners = new CopyOnWriteArrayList<>();

    private String currentUsername;
    private long currentUserId;
    private List<OnlineUserDto> onlineUsers = List.of();
    private List<RoomSummaryDto> rooms = List.of();
    private RoomStateDto room;
    private List<RankingEntryDto> rankingEntries = List.of();
    private List<MatchHistoryEntryDto> matchHistory = List.of();
    private final AtomicReference<GameStateDto> latestGameState = new AtomicReference<>();
    private final AtomicReference<GameOverDto> latestGameOver = new AtomicReference<>();
    private String feedback = "Connect to the server to begin";

    public void login(long userId, String username) {
        currentUserId = userId;
        currentUsername = username;
        feedback = "Logged in as " + username;
        notifyListeners();
    }

    public void logout() {
        currentUserId = 0;
        currentUsername = null;
        onlineUsers = List.of();
        rooms = List.of();
        room = null;
        rankingEntries = List.of();
        matchHistory = List.of();
        latestGameState.set(null);
        latestGameOver.set(null);
        feedback = "Logged out";
        notifyListeners();
    }

    public void setOnlineUsers(List<OnlineUserDto> onlineUsers) {
        this.onlineUsers = List.copyOf(onlineUsers);
        notifyListeners();
    }

    public void setRooms(List<RoomSummaryDto> rooms) {
        this.rooms = List.copyOf(rooms);
        notifyListeners();
    }

    public void setRoom(RoomStateDto room) {
        this.room = room;
        notifyListeners();
    }

    public void setRankingEntries(List<RankingEntryDto> rankingEntries) {
        this.rankingEntries = List.copyOf(rankingEntries);
        feedback = "Ranking updated";
        notifyListeners();
    }

    public void setMatchHistory(List<MatchHistoryEntryDto> matchHistory) {
        this.matchHistory = List.copyOf(matchHistory);
        feedback = "Match history updated";
        notifyListeners();
    }

    /**
     * Publishes an immutable authoritative snapshot from the network thread.
     * The render thread reads the same reference without mutating its contents.
     */
    public void setLatestGameState(GameStateDto gameState) {
        latestGameState.set(gameState);
        if (gameState.gameStatus() == GameStatus.RUNNING) {
            latestGameOver.set(null);
        }
    }

    public void setGameOver(GameOverDto gameOver) {
        latestGameOver.set(gameOver);
        feedback = "Game over";
        notifyListeners();
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
        notifyListeners();
    }

    public void addListener(ClientStateListener listener) {
        listeners.add(listener);
    }

    public void removeListener(ClientStateListener listener) {
        listeners.remove(listener);
    }

    public String getCurrentUsername() {
        return currentUsername;
    }

    public long getCurrentUserId() {
        return currentUserId;
    }

    public List<OnlineUserDto> getOnlineUsers() {
        return onlineUsers;
    }

    public List<RoomSummaryDto> getRooms() {
        return rooms;
    }

    public RoomStateDto getRoom() {
        return room;
    }

    public List<RankingEntryDto> getRankingEntries() {
        return rankingEntries;
    }

    public List<MatchHistoryEntryDto> getMatchHistory() {
        return matchHistory;
    }

    public GameStateDto getGameState() {
        return latestGameState.get();
    }

    public GameOverDto getGameOver() {
        return latestGameOver.get();
    }

    public String getFeedback() {
        return feedback;
    }

    private void notifyListeners() {
        listeners.forEach(ClientStateListener::onClientStateChanged);
    }
}
