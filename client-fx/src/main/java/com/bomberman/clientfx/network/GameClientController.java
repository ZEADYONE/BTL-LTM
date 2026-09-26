package com.bomberman.clientfx.network;

import com.bomberman.clientfx.state.ClientState;
import com.bomberman.clientfx.state.Feedback;
import com.bomberman.common.dto.CreateRoomRequest;
import com.bomberman.common.dto.JoinRoomRequest;
import com.bomberman.common.dto.LoginRequest;
import com.bomberman.common.dto.MoveRequest;
import com.bomberman.common.dto.ReadyRequest;
import com.bomberman.common.dto.RegisterRequest;
import com.bomberman.common.enums.Direction;
import com.bomberman.common.enums.MessageType;
import com.bomberman.common.message.NetworkMessage;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * UI-facing facade for all outgoing protocol commands; the socket is opened on first use.
 * Commands that the server answers return a future completed with the reply (or failed on
 * timeout / connection error) on the UI thread, which screens use to show a loading state.
 */
public final class GameClientController implements AutoCloseable {

    private final GameNetworkClient networkClient;
    private final ClientState state;
    private final PendingRequests pendingRequests;
    private final Executor uiThread;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExecutorService networkWriter = Executors.newSingleThreadExecutor(
            Thread.ofVirtual().name("client-network-writer", 0).factory());
    private volatile ClientNetworkConfig networkConfig;

    public GameClientController(
            GameNetworkClient networkClient,
            ClientState state,
            ClientNetworkConfig networkConfig,
            PendingRequests pendingRequests,
            Executor uiThread
    ) {
        this.networkClient = Objects.requireNonNull(networkClient, "networkClient must not be null");
        this.state = Objects.requireNonNull(state, "state must not be null");
        this.networkConfig = Objects.requireNonNull(networkConfig, "networkConfig must not be null");
        this.pendingRequests = Objects.requireNonNull(pendingRequests, "pendingRequests must not be null");
        this.uiThread = Objects.requireNonNull(uiThread, "uiThread must not be null");
    }

    public ClientNetworkConfig networkConfig() {
        return networkConfig;
    }

    /**
     * Points the client at another server (Server Address popup, logged out only).
     * An open connection to the old address is dropped; the next command connects to the new one.
     */
    public void setNetworkConfig(ClientNetworkConfig config) {
        ClientNetworkConfig previous = networkConfig;
        networkConfig = Objects.requireNonNull(config, "config must not be null");
        if (!config.equals(previous) && networkClient.isConnected()) {
            networkClient.close();
            state.setConnected(false);
        }
    }

    public CompletableFuture<NetworkMessage> login(String username, String password) {
        return request(MessageType.LOGIN_REQUEST, new LoginRequest(username, password), false);
    }

    public CompletableFuture<NetworkMessage> register(String username, String password) {
        return request(MessageType.REGISTER_REQUEST, new RegisterRequest(username, password), false);
    }

    public void logout() {
        send(MessageType.LOGOUT, null);
        state.logout();
    }

    public void requestLobbyState() {
        send(MessageType.ONLINE_USERS_REQUEST, null);
        send(MessageType.ROOM_LIST_REQUEST, null);
    }

    public CompletableFuture<NetworkMessage> createRoom(String roomName) {
        return request(MessageType.CREATE_ROOM, new CreateRoomRequest(roomName), false);
    }

    public CompletableFuture<NetworkMessage> joinRoom(String roomId) {
        return joinRoom(roomId, false);
    }

    /** @param quietErrors the caller retries or reports failures itself (Quick Play) */
    public CompletableFuture<NetworkMessage> joinRoom(String roomId, boolean quietErrors) {
        return request(MessageType.JOIN_ROOM, new JoinRoomRequest(roomId), quietErrors);
    }

    public CompletableFuture<NetworkMessage> leaveRoom() {
        return request(MessageType.LEAVE_ROOM, null, false);
    }

    public CompletableFuture<NetworkMessage> ready(boolean ready) {
        return request(MessageType.READY, new ReadyRequest(ready), false);
    }

    public CompletableFuture<NetworkMessage> startGame() {
        return request(MessageType.START_GAME, null, false);
    }

    public void move(Direction direction) {
        send(MessageType.MOVE, new MoveRequest(direction));
    }

    public void placeBomb() {
        send(MessageType.PLACE_BOMB, null);
    }

    public CompletableFuture<NetworkMessage> playAgain() {
        return request(MessageType.PLAY_AGAIN, null, false);
    }

    public CompletableFuture<NetworkMessage> requestRanking() {
        return request(MessageType.RANKING_REQUEST, null, false);
    }

    public CompletableFuture<NetworkMessage> requestHistory() {
        return request(MessageType.HISTORY_REQUEST, null, false);
    }

    private CompletableFuture<NetworkMessage> request(MessageType type, Object payload, boolean quietErrors) {
        NetworkMessage message = message(type, payload);
        CompletableFuture<NetworkMessage> reply = pendingRequests.register(message.requestId(), quietErrors);
        networkWriter.execute(() -> sendInBackground(message));
        return reply;
    }

    private void send(MessageType type, Object payload) {
        NetworkMessage message = message(type, payload);
        networkWriter.execute(() -> sendInBackground(message));
    }

    private NetworkMessage message(MessageType type, Object payload) {
        return new NetworkMessage(
                type,
                UUID.randomUUID().toString(),
                payload == null ? null : objectMapper.valueToTree(payload)
        );
    }

    private void sendInBackground(NetworkMessage message) {
        ClientNetworkConfig target = networkConfig;
        if (!networkClient.isConnected()) {
            try {
                networkClient.connect(target.host(), target.port());
                uiThread.execute(() -> state.setConnected(true));
            } catch (IOException | RuntimeException exception) {
                reportFailure(message, "Cannot connect to " + target.displayAddress() + ".", exception);
                return;
            }
        }
        try {
            networkClient.send(message);
        } catch (IOException | RuntimeException exception) {
            reportFailure(message, "Connection error: " + exception.getMessage(), exception);
        }
    }

    private void reportFailure(NetworkMessage message, String text, Exception cause) {
        uiThread.execute(() -> {
            pendingRequests.fail(message.requestId(), cause);
            state.setFeedback(Feedback.error(text));
        });
    }

    @Override
    public void close() {
        networkWriter.shutdownNow();
    }
}
