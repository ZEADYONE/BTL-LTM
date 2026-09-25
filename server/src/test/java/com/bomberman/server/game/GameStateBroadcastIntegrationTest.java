package com.bomberman.server.game;

import com.bomberman.common.dto.CreateRoomRequest;
import com.bomberman.common.dto.GamePlayerStateDto;
import com.bomberman.common.dto.GameOverDto;
import com.bomberman.common.dto.GameStateDto;
import com.bomberman.common.dto.JoinRoomRequest;
import com.bomberman.common.dto.LoginRequest;
import com.bomberman.common.dto.LoginResponse;
import com.bomberman.common.dto.MoveRequest;
import com.bomberman.common.dto.ReadyRequest;
import com.bomberman.common.dto.RegisterRequest;
import com.bomberman.common.dto.RegisterResponse;
import com.bomberman.common.dto.RoomStateUpdate;
import com.bomberman.common.enums.Direction;
import com.bomberman.common.enums.GameStatus;
import com.bomberman.common.enums.GameResult;
import com.bomberman.common.enums.MessageType;
import com.bomberman.common.message.NetworkMessage;
import com.bomberman.common.message.codec.MessageDecoder;
import com.bomberman.common.message.codec.MessageEncoder;
import com.bomberman.server.network.TcpGameServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "bomberman.tcp.port=0")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class GameStateBroadcastIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private TcpGameServer tcpGameServer;

    @Test
    @Timeout(40)
    void fourClientsReceiveIdenticalAuthoritativeSnapshots() throws Exception {
        List<TestGameClient> clients = new ArrayList<>();
        try {
            for (int index = 0; index < 4; index++) {
                TestGameClient client = new TestGameClient(tcpGameServer.getPort());
                clients.add(client);
                client.registerAndLogin("game_" + UUID.randomUUID().toString().substring(0, 12));
            }

            TestGameClient host = clients.getFirst();
            RoomStateUpdate createdRoom = host.request(
                    MessageType.CREATE_ROOM,
                    new CreateRoomRequest("Four Player Room"),
                    MessageType.ROOM_STATE,
                    RoomStateUpdate.class
            );
            String roomId = createdRoom.room().roomId();

            for (int index = 1; index < clients.size(); index++) {
                clients.get(index).request(
                        MessageType.JOIN_ROOM,
                        new JoinRoomRequest(roomId),
                        MessageType.ROOM_STATE,
                        RoomStateUpdate.class
                );
            }
            for (TestGameClient client : clients) {
                client.request(
                        MessageType.READY,
                        new ReadyRequest(true),
                        MessageType.ROOM_STATE,
                        RoomStateUpdate.class
                );
            }
            host.request(
                    MessageType.START_GAME,
                    null,
                    MessageType.ROOM_STATE,
                    RoomStateUpdate.class
            );

            List<GameStateDto> initialStates = clients.stream()
                    .map(client -> client.awaitGameState(state -> state.tick() >= 6))
                    .toList();
            assertAllEqual(initialStates);

            GameStateDto initial = initialStates.getFirst();
            assertEquals(GameStatus.RUNNING, initial.gameStatus());
            assertEquals(4, initial.players().size());
            assertEquals(4, initial.remainingPlayers());
            assertEquals(GameMap.ROWS, initial.map().size());
            assertEquals(GameMap.COLUMNS, initial.map().getFirst().size());

            long movingUserId = clients.getFirst().userId();
            long bombingUserId = clients.get(1).userId();
            clients.getFirst().send(MessageType.MOVE, new MoveRequest(Direction.RIGHT));
            clients.get(1).send(MessageType.PLACE_BOMB, null);

            Predicate<GameStateDto> containsProcessedCommands = state ->
                    state.tick() > initial.tick()
                            && player(state, movingUserId).position().column() == 2
                            && state.bombs().stream()
                                    .anyMatch(bomb -> bomb.ownerUserId() == bombingUserId);
            List<GameStateDto> updatedStates = clients.stream()
                    .map(client -> client.awaitGameState(containsProcessedCommands))
                    .toList();

            assertAllEqual(updatedStates);
            assertFalse(updatedStates.getFirst().bombs().isEmpty());
        } finally {
            for (TestGameClient client : clients) {
                client.close();
            }
        }
    }

    @Test
    @Timeout(70)
    void twoFourPlayerRoomsRunIndependentlyThroughGameOver() throws Exception {
        List<TestGameClient> clients = new ArrayList<>();
        try {
            for (int index = 0; index < 8; index++) {
                TestGameClient client = new TestGameClient(tcpGameServer.getPort());
                clients.add(client);
                client.registerAndLogin("multi_" + UUID.randomUUID().toString().substring(0, 12));
            }

            List<TestGameClient> roomOne = clients.subList(0, 4);
            List<TestGameClient> roomTwo = clients.subList(4, 8);
            String roomOneId = createAndStartRoom(roomOne, "Room One");
            String roomTwoId = createAndStartRoom(roomTwo, "Room Two");
            assertFalse(roomOneId.equals(roomTwoId));

            List<GameStateDto> roomOneInitial = awaitRoomState(roomOne, state -> state.tick() >= 4);
            List<GameStateDto> roomTwoInitial = awaitRoomState(roomTwo, state -> state.tick() >= 4);
            assertAllEqual(roomOneInitial);
            assertAllEqual(roomTwoInitial);
            assertRoomContainsOnly(roomOneInitial.getFirst(), roomOne);
            assertRoomContainsOnly(roomTwoInitial.getFirst(), roomTwo);

            roomOne.getFirst().send(MessageType.MOVE, new MoveRequest(Direction.RIGHT));
            roomOne.get(1).send(MessageType.PLACE_BOMB, null);
            roomTwo.getFirst().send(MessageType.MOVE, new MoveRequest(Direction.RIGHT));
            roomTwo.get(1).send(MessageType.PLACE_BOMB, null);

            long roomOneBombOwner = roomOne.get(1).userId();
            long roomTwoBombOwner = roomTwo.get(1).userId();
            List<GameStateDto> roomOneUpdated = awaitRoomState(roomOne, state ->
                    player(state, roomOne.getFirst().userId()).position().column() == 2
                            && state.bombs().stream().anyMatch(bomb ->
                            bomb.ownerUserId() == roomOneBombOwner)
            );
            List<GameStateDto> roomTwoUpdated = awaitRoomState(roomTwo, state ->
                    player(state, roomTwo.getFirst().userId()).position().column() == 2
                            && state.bombs().stream().anyMatch(bomb ->
                            bomb.ownerUserId() == roomTwoBombOwner)
            );
            assertAllEqual(roomOneUpdated);
            assertAllEqual(roomTwoUpdated);
            assertTrue(roomOneUpdated.getFirst().bombs().stream().noneMatch(bomb ->
                    bomb.ownerUserId() == roomTwoBombOwner));
            assertTrue(roomTwoUpdated.getFirst().bombs().stream().noneMatch(bomb ->
                    bomb.ownerUserId() == roomOneBombOwner));

            for (int index = 1; index < 4; index++) {
                roomOne.get(index).close();
                roomTwo.get(index).close();
            }
            GameStateDto roomOneFinal = roomOne.getFirst().awaitGameState(
                    state -> state.gameStatus() == GameStatus.FINISHED
            );
            GameStateDto roomTwoFinal = roomTwo.getFirst().awaitGameState(
                    state -> state.gameStatus() == GameStatus.FINISHED
            );
            assertEquals(1, roomOneFinal.remainingPlayers());
            assertEquals(1, roomTwoFinal.remainingPlayers());
            assertTrue(player(roomOneFinal, roomOne.getFirst().userId()).alive());
            assertTrue(player(roomTwoFinal, roomTwo.getFirst().userId()).alive());
            assertEquals(
                    GameResult.WIN,
                    roomOne.getFirst().awaitGameOver().players().stream()
                            .filter(player -> player.userId() == roomOne.getFirst().userId())
                            .findFirst()
                            .orElseThrow()
                            .result()
            );
            assertEquals(
                    GameResult.WIN,
                    roomTwo.getFirst().awaitGameOver().players().stream()
                            .filter(player -> player.userId() == roomTwo.getFirst().userId())
                            .findFirst()
                            .orElseThrow()
                            .result()
            );
        } finally {
            for (TestGameClient client : clients) {
                client.close();
            }
        }
    }

    private String createAndStartRoom(List<TestGameClient> clients, String roomName)
            throws IOException {
        TestGameClient host = clients.getFirst();
        RoomStateUpdate created = host.request(
                MessageType.CREATE_ROOM,
                new CreateRoomRequest(roomName),
                MessageType.ROOM_STATE,
                RoomStateUpdate.class
        );
        String roomId = created.room().roomId();
        for (int index = 1; index < clients.size(); index++) {
            clients.get(index).request(
                    MessageType.JOIN_ROOM,
                    new JoinRoomRequest(roomId),
                    MessageType.ROOM_STATE,
                    RoomStateUpdate.class
            );
        }
        for (TestGameClient client : clients) {
            client.request(
                    MessageType.READY,
                    new ReadyRequest(true),
                    MessageType.ROOM_STATE,
                    RoomStateUpdate.class
            );
        }
        host.request(
                MessageType.START_GAME,
                null,
                MessageType.ROOM_STATE,
                RoomStateUpdate.class
        );
        return roomId;
    }

    private List<GameStateDto> awaitRoomState(
            List<TestGameClient> clients,
            Predicate<GameStateDto> predicate
    ) {
        return clients.stream().map(client -> client.awaitGameState(predicate)).toList();
    }

    private void assertRoomContainsOnly(GameStateDto state, List<TestGameClient> clients) {
        List<Long> expectedUserIds = clients.stream().map(TestGameClient::userId).sorted().toList();
        List<Long> actualUserIds = state.players().stream()
                .map(GamePlayerStateDto::userId)
                .sorted()
                .toList();
        assertEquals(expectedUserIds, actualUserIds);
    }

    private void assertAllEqual(List<GameStateDto> states) {
        GameStateDto expected = states.getFirst();
        states.forEach(state -> {
            assertEquals(expected.tick(), state.tick());
            assertEquals(expected, state);
        });
    }

    private GamePlayerStateDto player(GameStateDto state, long userId) {
        return state.players().stream()
                .filter(player -> player.userId() == userId)
                .findFirst()
                .orElseThrow();
    }

    private final class TestGameClient implements AutoCloseable {

        private final Socket socket = new Socket();
        private final MessageEncoder encoder = new MessageEncoder(objectMapper);
        private final MessageDecoder decoder = new MessageDecoder(objectMapper);
        private long userId;

        private TestGameClient(int port) throws IOException {
            socket.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 2_000);
            socket.setSoTimeout(8_000);
        }

        private void registerAndLogin(String username) throws IOException {
            String password = "game-password";
            RegisterResponse registration = request(
                    MessageType.REGISTER_REQUEST,
                    new RegisterRequest(username, password),
                    MessageType.REGISTER_RESPONSE,
                    RegisterResponse.class
            );
            assertTrue(registration.success());
            LoginResponse login = request(
                    MessageType.LOGIN_REQUEST,
                    new LoginRequest(username, password),
                    MessageType.LOGIN_RESPONSE,
                    LoginResponse.class
            );
            assertTrue(login.success());
            userId = login.userId();
        }

        private long userId() {
            return userId;
        }

        private void send(MessageType type, Object payload) throws IOException {
            encoder.encode(
                    new NetworkMessage(
                            type,
                            UUID.randomUUID().toString(),
                            payload == null ? null : objectMapper.valueToTree(payload)
                    ),
                    socket.getOutputStream()
            );
        }

        private <T> T request(
                MessageType requestType,
                Object payload,
                MessageType responseType,
                Class<T> responseClass
        ) throws IOException {
            String requestId = UUID.randomUUID().toString();
            encoder.encode(
                    new NetworkMessage(
                            requestType,
                            requestId,
                            payload == null ? null : objectMapper.valueToTree(payload)
                    ),
                    socket.getOutputStream()
            );

            for (int attempt = 0; attempt < 200; attempt++) {
                NetworkMessage response = decoder.decode(socket.getInputStream());
                if (response.type() == MessageType.ERROR
                        && requestId.equals(response.requestId())) {
                    throw new IOException("Server rejected " + requestType + ": " + response.payload());
                }
                if (response.type() == responseType && requestId.equals(response.requestId())) {
                    return objectMapper.treeToValue(response.payload(), responseClass);
                }
            }
            throw new IOException("Expected response was not received: " + responseType);
        }

        private GameStateDto awaitGameState(Predicate<GameStateDto> predicate) {
            try {
                for (int attempt = 0; attempt < 200; attempt++) {
                    NetworkMessage message = decoder.decode(socket.getInputStream());
                    if (message.type() == MessageType.GAME_STATE) {
                        GameStateDto state = objectMapper.treeToValue(
                                message.payload(),
                                GameStateDto.class
                        );
                        if (predicate.test(state)) {
                            return state;
                        }
                    }
                }
                throw new IOException("Matching GAME_STATE was not received");
            } catch (IOException exception) {
                throw new IllegalStateException(exception);
            }
        }

        private GameOverDto awaitGameOver() throws IOException {
            for (int attempt = 0; attempt < 200; attempt++) {
                NetworkMessage message = decoder.decode(socket.getInputStream());
                if (message.type() == MessageType.GAME_OVER) {
                    return objectMapper.treeToValue(message.payload(), GameOverDto.class);
                }
            }
            throw new IOException("GAME_OVER was not received");
        }

        @Override
        public void close() throws IOException {
            socket.close();
        }
    }
}
