package com.bomberman.client.network;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.bomberman.client.screen.ClientNavigator;
import com.bomberman.client.state.ClientState;
import com.bomberman.common.dto.ErrorResponse;
import com.bomberman.common.dto.GameStateDto;
import com.bomberman.common.dto.GameOverDto;
import com.bomberman.common.dto.LoginResponse;
import com.bomberman.common.dto.OnlineUsersUpdate;
import com.bomberman.common.dto.RegisterResponse;
import com.bomberman.common.dto.RoomListUpdate;
import com.bomberman.common.dto.RoomStateUpdate;
import com.bomberman.common.dto.RankingResponse;
import com.bomberman.common.dto.HistoryResponse;
import com.bomberman.common.enums.RoomStatus;
import com.bomberman.common.message.NetworkMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Marshals server callbacks onto the libGDX application thread before changing UI state. */
public final class ClientMessageDispatcher implements ServerListener {

    private final ClientState state;
    private final ClientNavigator navigator;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ClientMessageDispatcher(ClientState state, ClientNavigator navigator) {
        this.state = state;
        this.navigator = navigator;
    }

    @Override
    public void onMessage(NetworkMessage message) {
        if (message.type() == com.bomberman.common.enums.MessageType.GAME_STATE) {
            handleGameStateFromNetworkThread(message);
            return;
        }
        postToUiThread(() -> dispatchOnUiThread(message));
    }

    @Override
    public void onDisconnected() {
        postToUiThread(() -> {
            state.logout();
            state.setFeedback("Disconnected from server");
            navigator.showLogin();
        });
    }

    @Override
    public void onError(Throwable error) {
        postToUiThread(() -> state.setFeedback("Network error: " + error.getMessage()));
    }

    private void dispatchOnUiThread(NetworkMessage message) {
        try {
            switch (message.type()) {
                case REGISTER_RESPONSE -> handleRegister(read(message, RegisterResponse.class));
                case LOGIN_RESPONSE -> handleLogin(read(message, LoginResponse.class));
                case ONLINE_USERS_UPDATE -> state.setOnlineUsers(
                        read(message, OnlineUsersUpdate.class).users()
                );
                case ROOM_LIST_UPDATE -> state.setRooms(read(message, RoomListUpdate.class).rooms());
                case ROOM_STATE -> handleRoomState(read(message, RoomStateUpdate.class));
                case GAME_STATE -> {
                    // GAME_STATE is decoded before dispatch and published through AtomicReference.
                }
                case ERROR -> {
                    ErrorResponse error = read(message, ErrorResponse.class);
                    state.setFeedback(error.code() + ": " + error.message());
                }
                case GAME_OVER -> state.setGameOver(read(message, GameOverDto.class));
                case RANKING_RESPONSE -> state.setRankingEntries(
                        read(message, RankingResponse.class).entries()
                );
                case HISTORY_RESPONSE -> state.setMatchHistory(
                        read(message, HistoryResponse.class).matches()
                );
                default -> {
                    // Other messages do not currently alter presentation state.
                }
            }
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            state.setFeedback("Invalid server message: " + message.type());
        }
    }

    private void handleRegister(RegisterResponse response) {
        state.setFeedback(response.success()
                ? "Registration successful. You can now log in."
                : "Registration failed: " + response.result());
    }

    private void handleLogin(LoginResponse response) {
        if (response.success()) {
            state.login(response.userId(), response.username());
            navigator.showLobby();
        } else {
            state.setFeedback("Login failed: " + response.result());
        }
    }

    private void handleRoomState(RoomStateUpdate update) {
        state.setRoom(update.room());
        if (!update.member()) {
            navigator.showLobby();
        } else if (update.room() != null && update.room().status() == RoomStatus.PLAYING) {
            navigator.showGame();
        } else {
            navigator.showRoom();
        }
    }

    private void handleGameStateFromNetworkThread(NetworkMessage message) {
        try {
            GameStateDto gameState = read(message, GameStateDto.class);
            state.setLatestGameState(gameState);
            postToUiThread(navigator::showGame);
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            postToUiThread(() -> state.setFeedback("Invalid server message: " + message.type()));
        }
    }

    private <T> T read(NetworkMessage message, Class<T> type) throws JsonProcessingException {
        if (message.payload() == null) {
            throw new IllegalArgumentException("Missing payload");
        }
        return objectMapper.treeToValue(message.payload(), type);
    }

    private void postToUiThread(Runnable action) {
        Application application = Gdx.app;
        if (application != null) {
            application.postRunnable(action);
        }
    }
}
