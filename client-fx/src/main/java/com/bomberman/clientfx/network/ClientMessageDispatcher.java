package com.bomberman.clientfx.network;

import com.bomberman.clientfx.state.ClientState;
import com.bomberman.clientfx.ui.Navigator;
import com.bomberman.clientfx.ui.ScreenId;
import com.bomberman.common.dto.ErrorResponse;
import com.bomberman.common.dto.GameOverDto;
import com.bomberman.common.dto.GameStateDto;
import com.bomberman.common.dto.HistoryResponse;
import com.bomberman.common.dto.LoginResponse;
import com.bomberman.common.dto.OnlineUsersUpdate;
import com.bomberman.common.dto.RankingResponse;
import com.bomberman.common.dto.RegisterResponse;
import com.bomberman.common.dto.RoomListUpdate;
import com.bomberman.common.dto.RoomStateUpdate;
import com.bomberman.common.enums.AuthResultCode;
import com.bomberman.common.enums.GameCommandResultCode;
import com.bomberman.common.enums.MessageType;
import com.bomberman.common.enums.RoomStatus;
import com.bomberman.common.message.NetworkMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * Applies server messages to {@link ClientState} and switches screens. Everything except
 * {@code GAME_STATE} is handed to the UI thread; snapshots are decoded on the network thread
 * so the render loop only ever reads a finished object.
 */
public final class ClientMessageDispatcher implements ServerListener {

    private static final System.Logger LOG = System.getLogger(ClientMessageDispatcher.class.getName());

    private final ClientState state;
    private final Navigator navigator;
    private final Executor uiThread;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ClientMessageDispatcher(ClientState state, Navigator navigator, Executor uiThread) {
        this.state = Objects.requireNonNull(state, "state must not be null");
        this.navigator = Objects.requireNonNull(navigator, "navigator must not be null");
        this.uiThread = Objects.requireNonNull(uiThread, "uiThread must not be null");
    }

    @Override
    public void onMessage(NetworkMessage message) {
        if (message.type() == MessageType.GAME_STATE) {
            handleGameStateFromNetworkThread(message);
            return;
        }
        uiThread.execute(() -> dispatchOnUiThread(message));
    }

    @Override
    public void onDisconnected() {
        uiThread.execute(() -> {
            state.logout();
            state.setFeedback("Disconnected from server.");
            navigator.show(ScreenId.LOGIN);
        });
    }

    @Override
    public void onError(Throwable error) {
        // onDisconnected always follows and tells the player; keep the cause for debugging.
        LOG.log(System.Logger.Level.WARNING, "Connection failed", error);
    }

    private void dispatchOnUiThread(NetworkMessage message) {
        try {
            switch (message.type()) {
                case REGISTER_RESPONSE -> handleRegister(read(message, RegisterResponse.class));
                case LOGIN_RESPONSE -> handleLogin(read(message, LoginResponse.class));
                case ONLINE_USERS_UPDATE -> state.setOnlineUsers(read(message, OnlineUsersUpdate.class).users());
                case ROOM_LIST_UPDATE -> state.setRooms(read(message, RoomListUpdate.class).rooms());
                case ROOM_STATE -> handleRoomState(read(message, RoomStateUpdate.class));
                case GAME_OVER -> state.setGameOver(read(message, GameOverDto.class));
                case RANKING_RESPONSE -> state.setRankingEntries(read(message, RankingResponse.class).entries());
                case HISTORY_RESPONSE -> state.setMatchHistory(read(message, HistoryResponse.class).matches());
                case ERROR -> handleError(read(message, ErrorResponse.class));
                default -> {
                    // Other messages do not change presentation state.
                }
            }
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            state.setFeedback("Invalid server message: " + message.type());
        }
    }

    private void handleRegister(RegisterResponse response) {
        state.setFeedback(response.success()
                ? "Account created. You can log in now."
                : describe(response.result()));
    }

    private void handleLogin(LoginResponse response) {
        if (response.success()) {
            state.login(response.userId(), response.username());
            navigator.show(ScreenId.HOME);
        } else {
            state.setFeedback(describe(response.result()));
        }
    }

    private void handleRoomState(RoomStateUpdate update) {
        if (!update.member() || update.room() == null) {
            state.setRoom(null);
            navigator.show(ScreenId.HOME);
            return;
        }
        state.setRoom(update.room());
        RoomStatus status = update.room().status();
        if (status == RoomStatus.PLAYING) {
            navigator.show(ScreenId.GAME);
        } else if (status == RoomStatus.WAITING && navigator.current() != ScreenId.RESULT) {
            // A rematch started by another player must not pull this player off the result screen.
            navigator.show(ScreenId.ROOM_LOBBY);
        }
    }

    private void handleError(ErrorResponse error) {
        boolean gameCommandError = Arrays.stream(GameCommandResultCode.values())
                .anyMatch(code -> code.name().equals(error.code()));
        if (gameCommandError && navigator.current() == ScreenId.GAME) {
            // e.g. moving after being knocked out; not worth interrupting the match.
            return;
        }
        String message = error.message();
        state.setFeedback(message == null || message.isBlank() ? error.code() : message);
    }

    private void handleGameStateFromNetworkThread(NetworkMessage message) {
        try {
            state.setLatestGameState(read(message, GameStateDto.class));
            uiThread.execute(() -> {
                ScreenId current = navigator.current();
                if (current != ScreenId.GAME && current != ScreenId.RESULT) {
                    navigator.show(ScreenId.GAME);
                }
            });
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            uiThread.execute(() -> state.setFeedback("Invalid server message: " + message.type()));
        }
    }

    private <T> T read(NetworkMessage message, Class<T> type) throws JsonProcessingException {
        if (message.payload() == null) {
            throw new IllegalArgumentException("Missing payload");
        }
        return objectMapper.treeToValue(message.payload(), type);
    }

    static String describe(AuthResultCode result) {
        if (result == null) {
            return "Invalid request. Please try again.";
        }
        return switch (result) {
            case SUCCESS -> "Success.";
            case INVALID_REQUEST -> "Invalid request. Please try again.";
            case INVALID_USERNAME -> "Username must be 1–50 characters.";
            case INVALID_PASSWORD -> "Password is required.";
            case USERNAME_ALREADY_EXISTS -> "That username is already taken.";
            case INVALID_CREDENTIALS -> "Wrong username or password.";
            case ACCOUNT_ALREADY_ONLINE -> "This account is already online on another device.";
            case SESSION_ALREADY_AUTHENTICATED -> "You are already logged in.";
        };
    }
}
