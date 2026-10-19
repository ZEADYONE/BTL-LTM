package com.bomberman.clientfx.network;

import com.bomberman.clientfx.state.ClientState;
import com.bomberman.clientfx.state.Feedback;
import com.bomberman.clientfx.ui.Navigator;
import com.bomberman.clientfx.ui.ScreenId;
import com.bomberman.common.dto.ErrorResponse;
import com.bomberman.common.dto.FriendListUpdate;
import com.bomberman.common.dto.FriendRequestDto;
import com.bomberman.common.dto.GameOverDto;
import com.bomberman.common.dto.GameStateDto;
import com.bomberman.common.dto.HistoryResponse;
import com.bomberman.common.dto.LoginResponse;
import com.bomberman.common.dto.OnlineUsersUpdate;
import com.bomberman.common.dto.RankingResponse;
import com.bomberman.common.dto.RegisterResponse;
import com.bomberman.common.dto.RoomMemberRemovedNotice;
import com.bomberman.common.dto.RoomListUpdate;
import com.bomberman.common.dto.RoomStateUpdate;
import com.bomberman.common.dto.RoomInviteDto;
import com.bomberman.common.dto.RoomInviteStatusUpdate;
import com.bomberman.common.enums.AuthResultCode;
import com.bomberman.common.enums.GameCommandResultCode;
import com.bomberman.common.enums.MessageType;
import com.bomberman.common.enums.RoomStatus;
import com.bomberman.common.enums.TileType;
import com.bomberman.common.message.NetworkMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

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
    private final PendingRequests pendingRequests;
    private final Runnable onConnectionLost;
    private final ObjectMapper objectMapper = new ObjectMapper();
    /** Last map received; only the network thread touches it. */
    private List<List<TileType>> lastMap;

    /** @param onConnectionLost runs on the UI thread after an established connection drops */
    public ClientMessageDispatcher(
            ClientState state,
            Navigator navigator,
            Executor uiThread,
            PendingRequests pendingRequests,
            Runnable onConnectionLost
    ) {
        this.state = Objects.requireNonNull(state, "state must not be null");
        this.navigator = Objects.requireNonNull(navigator, "navigator must not be null");
        this.uiThread = Objects.requireNonNull(uiThread, "uiThread must not be null");
        this.pendingRequests = Objects.requireNonNull(pendingRequests, "pendingRequests must not be null");
        this.onConnectionLost = Objects.requireNonNull(onConnectionLost, "onConnectionLost must not be null");
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
            state.setConnected(false);
            pendingRequests.failAll(new IOException("Connection lost"));
            navigator.show(ScreenId.LOGIN);
            onConnectionLost.run();
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
                case FRIEND_LIST_UPDATE -> handleFriendList(
                        message,
                        read(message, FriendListUpdate.class)
                );
                case ROOM_LIST_UPDATE -> state.setRooms(read(message, RoomListUpdate.class).rooms());
                case ROOM_MEMBER_REMOVED -> handleRoomMemberRemoved(
                        read(message, RoomMemberRemovedNotice.class)
                );
                case ROOM_INVITE_RECEIVED -> state.addRoomInvite(read(message, RoomInviteDto.class));
                case ROOM_INVITE_STATUS -> handleRoomInviteStatus(
                        message,
                        read(message, RoomInviteStatusUpdate.class)
                );
                case ROOM_STATE -> handleRoomState(read(message, RoomStateUpdate.class));
                case GAME_OVER -> state.setGameOver(read(message, GameOverDto.class));
                case RANKING_RESPONSE -> state.setRankingEntries(read(message, RankingResponse.class).entries());
                case HISTORY_RESPONSE -> state.setMatchHistory(read(message, HistoryResponse.class).matches());
                case ERROR -> {
                    if (!pendingRequests.isQuiet(message.requestId())) {
                        handleError(read(message, ErrorResponse.class));
                    }
                }
                default -> {
                    // Other messages do not change presentation state.
                }
            }
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            state.setFeedback(Feedback.error("Invalid server message: " + message.type()));
        }
        pendingRequests.complete(message);
    }

    private void handleRegister(RegisterResponse response) {
        state.setFeedback(response.success()
                ? Feedback.success("Account created. You can log in now.")
                : Feedback.error(describe(response.result())));
    }

    private void handleFriendList(NetworkMessage message, FriendListUpdate update) {
        Set<Long> previousIncoming = state.getIncomingFriendRequests().stream()
                .map(FriendRequestDto::relationshipId)
                .collect(Collectors.toSet());
        state.setFriendList(update);
        if (message.requestId() != null) {
            return;
        }
        update.incomingRequests().stream()
                .filter(request -> !previousIncoming.contains(request.relationshipId()))
                .findFirst()
                .ifPresent(request -> state.setFeedback(
                        Feedback.info("Friend request from " + request.username())
                ));
    }

    private void handleLogin(LoginResponse response) {
        if (response.success()) {
            state.login(response.userId(), response.username());
            navigator.show(ScreenId.HOME);
        } else {
            state.setFeedback(Feedback.error(describe(response.result())));
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
            state.beginGame();
            navigator.show(ScreenId.GAME);
        } else if (status == RoomStatus.WAITING && navigator.current() != ScreenId.RESULT) {
            // A rematch started by another player must not pull this player off the result screen.
            navigator.show(ScreenId.ROOM_LOBBY);
        }
    }

    private void handleRoomMemberRemoved(RoomMemberRemovedNotice notice) {
        state.setFeedback(Feedback.info(
                "Host " + notice.actorUsername() + " removed you from " + notice.roomName()
        ));
    }

    private void handleRoomInviteStatus(NetworkMessage message, RoomInviteStatusUpdate update) {
        state.removeRoomInvite(update.inviteId());
        if (message.requestId() != null) {
            return;
        }
        switch (update.status()) {
            case ACCEPTED -> state.setFeedback(Feedback.success("Room invitation accepted."));
            case DECLINED -> state.setFeedback(Feedback.info("Room invitation declined."));
            case EXPIRED -> state.setFeedback(Feedback.info("Room invitation expired."));
            case CANCELLED -> { /* lifecycle cancellation is intentionally quiet */ }
            case SENT -> { /* only sent as a correlated command response */ }
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
        state.setFeedback(Feedback.error(message == null || message.isBlank() ? error.code() : message));
    }

    private void handleGameStateFromNetworkThread(NetworkMessage message) {
        try {
            GameStateDto snapshot = withKnownMap(read(message, GameStateDto.class));
            if (snapshot == null) {
                return;
            }
            state.setLatestGameState(snapshot);
            uiThread.execute(() -> {
                ScreenId current = navigator.current();
                // The server keeps sending snapshots to a player who just left until the match ends;
                // only players still in a room are taken to the match.
                if (state.getRoom() != null && current != ScreenId.GAME && current != ScreenId.RESULT) {
                    navigator.show(ScreenId.GAME);
                }
            });
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            uiThread.execute(() -> state.setFeedback(Feedback.error("Invalid server message: " + message.type())));
        }
    }

    /** Fills in a map the server left out because it had not changed; null until a map arrived. */
    private GameStateDto withKnownMap(GameStateDto snapshot) {
        if (snapshot.map() != null) {
            lastMap = snapshot.map();
            return snapshot;
        }
        return lastMap == null ? null : snapshot.withMap(lastMap);
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
