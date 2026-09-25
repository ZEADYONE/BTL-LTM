package com.bomberman.server.game;

import com.bomberman.common.dto.CreateRoomRequest;
import com.bomberman.common.dto.GameStateDto;
import com.bomberman.common.dto.HistoryResponse;
import com.bomberman.common.dto.JoinRoomRequest;
import com.bomberman.common.dto.LoginRequest;
import com.bomberman.common.dto.LoginResponse;
import com.bomberman.common.dto.RankingResponse;
import com.bomberman.common.dto.ReadyRequest;
import com.bomberman.common.dto.RegisterRequest;
import com.bomberman.common.dto.RoomStateUpdate;
import com.bomberman.common.enums.GameResult;
import com.bomberman.common.enums.GameStatus;
import com.bomberman.common.enums.MessageType;
import com.bomberman.common.message.NetworkMessage;
import com.bomberman.common.message.codec.MessageDecoder;
import com.bomberman.common.message.codec.MessageEncoder;
import com.bomberman.server.network.TcpGameServer;
import com.bomberman.server.repository.MatchRepository;
import com.bomberman.server.user.OnlineUserRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "bomberman.tcp.port=0")
@Timeout(35)
class DisconnectDuringGameIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private TcpGameServer tcpGameServer;

    @Autowired
    private OnlineUserRegistry onlineUserRegistry;

    @Autowired
    private MatchRepository matchRepository;

    @Test
    void disconnectKillsPlayerPersistsWinAndExposesRankingAndHistory() throws Exception {
        try (TestClient host = new TestClient(); TestClient guest = new TestClient()) {
            host.registerAndLogin(uniqueUsername("host"));
            guest.registerAndLogin(uniqueUsername("guest"));

            RoomStateUpdate created = host.request(
                    MessageType.CREATE_ROOM,
                    new CreateRoomRequest("Disconnect Room"),
                    MessageType.ROOM_STATE,
                    RoomStateUpdate.class
            );
            String roomId = created.room().roomId();
            guest.request(
                    MessageType.JOIN_ROOM,
                    new JoinRoomRequest(roomId),
                    MessageType.ROOM_STATE,
                    RoomStateUpdate.class
            );
            host.request(
                    MessageType.READY,
                    new ReadyRequest(true),
                    MessageType.ROOM_STATE,
                    RoomStateUpdate.class
            );
            guest.request(
                    MessageType.READY,
                    new ReadyRequest(true),
                    MessageType.ROOM_STATE,
                    RoomStateUpdate.class
            );
            host.request(
                    MessageType.START_GAME,
                    null,
                    MessageType.ROOM_STATE,
                    RoomStateUpdate.class
            );
            host.awaitGameState(state -> state.gameStatus() == GameStatus.RUNNING);
            guest.awaitGameState(state -> state.gameStatus() == GameStatus.RUNNING);

            long disconnectedUserId = guest.userId;
            guest.close();

            GameStateDto finalState = host.awaitGameState(
                    state -> state.gameStatus() == GameStatus.FINISHED
            );
            assertEquals(1, finalState.remainingPlayers());
            assertTrue(playerAlive(finalState, host.userId));
            assertFalse(playerAlive(finalState, disconnectedUserId));
            assertTrue(waitUntil(
                    () -> !onlineUserRegistry.isOnline(disconnectedUserId),
                    Duration.ofSeconds(3)
            ));
            assertTrue(waitUntil(
                    () -> matchRepository.findByRoomId(roomId).isPresent(),
                    Duration.ofSeconds(3)
            ));
            var match = matchRepository.findByRoomId(roomId).orElseThrow();
            assertEquals(GameResult.WIN, match.getResult());
            assertEquals(host.userId, match.getWinnerUserId());

            RankingResponse ranking = host.request(
                    MessageType.RANKING_REQUEST,
                    null,
                    MessageType.RANKING_RESPONSE,
                    RankingResponse.class
            );
            assertTrue(ranking.entries().stream().anyMatch(entry ->
                    entry.userId() == host.userId && entry.totalScoreUnits() >= 2
            ));

            HistoryResponse history = host.request(
                    MessageType.HISTORY_REQUEST,
                    null,
                    MessageType.HISTORY_RESPONSE,
                    HistoryResponse.class
            );
            assertTrue(history.matches().stream().anyMatch(entry ->
                    entry.roomId().equals(roomId) && entry.viewerResult() == GameResult.WIN
            ));
        }
    }

    private boolean playerAlive(GameStateDto state, long userId) {
        return state.players().stream()
                .filter(player -> player.userId() == userId)
                .findFirst()
                .orElseThrow()
                .alive();
    }

    private boolean waitUntil(BooleanSupplier condition, Duration timeout)
            throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        return condition.getAsBoolean();
    }

    private String uniqueUsername(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private final class TestClient implements AutoCloseable {

        private final Socket socket = new Socket();
        private final MessageEncoder encoder = new MessageEncoder(objectMapper);
        private final MessageDecoder decoder = new MessageDecoder(objectMapper);
        private long userId;

        private TestClient() throws IOException {
            socket.connect(new InetSocketAddress(
                    InetAddress.getLoopbackAddress(),
                    tcpGameServer.getPort()
            ), 2_000);
            socket.setSoTimeout(8_000);
        }

        private void registerAndLogin(String username) throws IOException {
            request(
                    MessageType.REGISTER_REQUEST,
                    new RegisterRequest(username, "password"),
                    MessageType.REGISTER_RESPONSE,
                    com.bomberman.common.dto.RegisterResponse.class
            );
            LoginResponse login = request(
                    MessageType.LOGIN_REQUEST,
                    new LoginRequest(username, "password"),
                    MessageType.LOGIN_RESPONSE,
                    LoginResponse.class
            );
            userId = login.userId();
        }

        private <T> T request(
                MessageType requestType,
                Object payload,
                MessageType responseType,
                Class<T> responseClass
        ) throws IOException {
            String requestId = UUID.randomUUID().toString();
            encoder.encode(new NetworkMessage(
                    requestType,
                    requestId,
                    payload == null ? null : objectMapper.valueToTree(payload)
            ), socket.getOutputStream());
            for (int attempt = 0; attempt < 200; attempt++) {
                NetworkMessage response = decoder.decode(socket.getInputStream());
                if (response.type() == responseType && requestId.equals(response.requestId())) {
                    return objectMapper.treeToValue(response.payload(), responseClass);
                }
            }
            throw new IOException("Expected response not received: " + responseType);
        }

        private GameStateDto awaitGameState(
                java.util.function.Predicate<GameStateDto> predicate
        ) throws IOException {
            for (int attempt = 0; attempt < 200; attempt++) {
                NetworkMessage response = decoder.decode(socket.getInputStream());
                if (response.type() == MessageType.GAME_STATE) {
                    GameStateDto state = objectMapper.treeToValue(
                            response.payload(),
                            GameStateDto.class
                    );
                    if (predicate.test(state)) {
                        return state;
                    }
                }
            }
            throw new IOException("Expected GAME_STATE not received");
        }

        @Override
        public void close() throws IOException {
            if (!socket.isClosed()) {
                socket.close();
            }
        }
    }
}
