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
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** UI-facing facade for all outgoing protocol commands; the socket is opened on first use. */
public final class GameClientController implements AutoCloseable {

    private final GameNetworkClient networkClient;
    private final ClientState state;
    private final ClientNetworkConfig networkConfig;
    private final Executor uiThread;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExecutorService networkWriter = Executors.newSingleThreadExecutor(
            Thread.ofVirtual().name("client-network-writer", 0).factory());

    public GameClientController(
            GameNetworkClient networkClient,
            ClientState state,
            ClientNetworkConfig networkConfig,
            Executor uiThread
    ) {
        this.networkClient = Objects.requireNonNull(networkClient, "networkClient must not be null");
        this.state = Objects.requireNonNull(state, "state must not be null");
        this.networkConfig = Objects.requireNonNull(networkConfig, "networkConfig must not be null");
        this.uiThread = Objects.requireNonNull(uiThread, "uiThread must not be null");
    }

    public ClientNetworkConfig networkConfig() {
        return networkConfig;
    }

    public void login(String username, String password) {
        send(MessageType.LOGIN_REQUEST, new LoginRequest(username, password));
    }

    public void register(String username, String password) {
        send(MessageType.REGISTER_REQUEST, new RegisterRequest(username, password));
    }

    public void logout() {
        send(MessageType.LOGOUT, null);
        state.logout();
    }

    public void requestLobbyState() {
        send(MessageType.ONLINE_USERS_REQUEST, null);
        send(MessageType.ROOM_LIST_REQUEST, null);
    }

    public void createRoom(String roomName) {
        send(MessageType.CREATE_ROOM, new CreateRoomRequest(roomName));
    }

    public void joinRoom(String roomId) {
        send(MessageType.JOIN_ROOM, new JoinRoomRequest(roomId));
    }

    public void leaveRoom() {
        send(MessageType.LEAVE_ROOM, null);
    }

    public void ready(boolean ready) {
        send(MessageType.READY, new ReadyRequest(ready));
    }

    public void startGame() {
        send(MessageType.START_GAME, null);
    }

    public void move(Direction direction) {
        send(MessageType.MOVE, new MoveRequest(direction));
    }

    public void placeBomb() {
        send(MessageType.PLACE_BOMB, null);
    }

    public void playAgain() {
        send(MessageType.PLAY_AGAIN, null);
    }

    public void requestRanking() {
        send(MessageType.RANKING_REQUEST, null);
    }

    public void requestHistory() {
        send(MessageType.HISTORY_REQUEST, null);
    }

    private void send(MessageType type, Object payload) {
        NetworkMessage message = new NetworkMessage(
                type,
                UUID.randomUUID().toString(),
                payload == null ? null : objectMapper.valueToTree(payload)
        );
        networkWriter.execute(() -> sendInBackground(message));
    }

    private void sendInBackground(NetworkMessage message) {
        if (!networkClient.isConnected()) {
            try {
                networkClient.connect(networkConfig.host(), networkConfig.port());
            } catch (IOException | RuntimeException exception) {
                postError("Cannot connect to " + networkConfig.displayAddress() + ".");
                return;
            }
        }
        try {
            networkClient.send(message);
        } catch (IOException | RuntimeException exception) {
            postError("Connection error: " + exception.getMessage());
        }
    }

    private void postError(String message) {
        uiThread.execute(() -> state.setFeedback(Feedback.error(message)));
    }

    @Override
    public void close() {
        networkWriter.shutdownNow();
    }
}
