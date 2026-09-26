package com.bomberman.clientfx.state;

import com.bomberman.common.dto.GameOverDto;
import com.bomberman.common.dto.GameStateDto;
import com.bomberman.common.dto.MatchHistoryEntryDto;
import com.bomberman.common.dto.OnlineUserDto;
import com.bomberman.common.dto.RankingEntryDto;
import com.bomberman.common.dto.RoomStateDto;
import com.bomberman.common.dto.RoomSummaryDto;
import com.bomberman.common.enums.GameStatus;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Mutable presentation state. Everything except the game snapshot is changed on the
 * JavaFX application thread; the snapshot is published by the network thread.
 */
public final class ClientState {

    private final List<ClientStateListener> listeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<Feedback>> feedbackListeners = new CopyOnWriteArrayList<>();

    private boolean connected;
    private String currentUsername;
    private long currentUserId;
    private List<OnlineUserDto> onlineUsers = List.of();
    private List<RoomSummaryDto> rooms = List.of();
    private RoomStateDto room;
    private List<RankingEntryDto> rankingEntries = List.of();
    private List<MatchHistoryEntryDto> matchHistory = List.of();
    private final AtomicReference<GameStateDto> latestGameState = new AtomicReference<>();
    private final AtomicReference<GameOverDto> latestGameOver = new AtomicReference<>();

    public void login(long userId, String username) {
        currentUserId = userId;
        currentUsername = username;
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
        notifyListeners();
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
        notifyListeners();
    }

    public boolean isConnected() {
        return connected;
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
        notifyListeners();
    }

    public void setMatchHistory(List<MatchHistoryEntryDto> matchHistory) {
        this.matchHistory = List.copyOf(matchHistory);
        notifyListeners();
    }

    /**
     * Publishes an immutable authoritative snapshot from the network thread.
     * The render loop reads the same reference without mutating its contents.
     */
    public void setLatestGameState(GameStateDto gameState) {
        latestGameState.set(gameState);
        if (gameState.gameStatus() == GameStatus.RUNNING) {
            latestGameOver.set(null);
        }
    }

    /** Drops snapshots from the previous round before the first snapshot of a new game arrives. */
    public void beginGame() {
        GameStateDto current = latestGameState.get();
        // GAME_STATE is decoded on the network thread and can overtake this UI-thread ROOM_STATE.
        // Preserve an already-arrived RUNNING snapshot unless GAME_OVER proves it belongs to the old round.
        if (latestGameOver.get() != null || current == null || current.gameStatus() != GameStatus.RUNNING) {
            latestGameState.set(null);
        }
        latestGameOver.set(null);
    }

    public void setGameOver(GameOverDto gameOver) {
        latestGameOver.set(gameOver);
        notifyListeners();
    }

    /** Announces a one-off message (result or error) to the player. */
    public void setFeedback(Feedback feedback) {
        feedbackListeners.forEach(listener -> listener.accept(feedback));
    }

    public void addListener(ClientStateListener listener) {
        listeners.add(listener);
    }

    public void removeListener(ClientStateListener listener) {
        listeners.remove(listener);
    }

    public void addFeedbackListener(Consumer<Feedback> listener) {
        feedbackListeners.add(listener);
    }

    public boolean isLoggedIn() {
        return currentUsername != null;
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

    private void notifyListeners() {
        listeners.forEach(ClientStateListener::onClientStateChanged);
    }
}
