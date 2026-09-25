package com.bomberman.client.network;

import com.badlogic.gdx.Gdx;
import com.bomberman.common.dto.CreateRoomRequest;
import com.bomberman.common.dto.JoinRoomRequest;
import com.bomberman.common.dto.LoginRequest;
import com.bomberman.common.dto.MoveRequest;
import com.bomberman.common.dto.ReadyRequest;
import com.bomberman.common.dto.RegisterRequest;
import com.bomberman.common.enums.Direction;
import com.bomberman.common.enums.MessageType;
import com.bomberman.common.message.NetworkMessage;
import com.bomberman.client.state.ClientState;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** UI-facing facade for all outgoing protocol commands. */
public final class GameClientController implements AutoCloseable {

    private final GameNetworkClient networkClient;
    private final ClientState state;
    private final ClientNetworkConfig networkConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExecutorService networkWriter = Executors.newSingleThreadExecutor(
            Thread.ofVirtual().name("client-network-writer", 0).factory());

    public GameClientController(
            GameNetworkClient networkClient,
            ClientState state,
            ClientNetworkConfig networkConfig
    ) {
        this.networkClient = networkClient;
        this.state = state;
        this.networkConfig = networkConfig;
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
        try {
            ensureConnected();
            networkClient.send(message);
        } catch (IOException | RuntimeException exception) {
            postFeedback("Connection error: " + exception.getMessage());
        }
    }

    private void ensureConnected() throws IOException {
        if (!networkClient.isConnected()) {
            networkClient.connect(networkConfig.host(), networkConfig.port());
            postFeedback("Connected to " + networkConfig.host() + ":" + networkConfig.port());
        }
    }

    private void postFeedback(String feedback) {
        Gdx.app.postRunnable(() -> state.setFeedback(feedback));
    }

    @Override
    public void close() {
        networkWriter.shutdownNow();
    }
}
