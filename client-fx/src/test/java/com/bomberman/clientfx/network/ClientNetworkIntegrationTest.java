package com.bomberman.clientfx.network;

import com.bomberman.clientfx.state.ClientState;
import com.bomberman.clientfx.ui.Navigator;
import com.bomberman.clientfx.ui.ScreenId;
import com.bomberman.common.dto.LoginRequest;
import com.bomberman.common.dto.LoginResponse;
import com.bomberman.common.enums.AuthResultCode;
import com.bomberman.common.enums.MessageType;
import com.bomberman.common.enums.PlayerStatus;
import com.bomberman.common.message.NetworkMessage;
import com.bomberman.common.message.codec.MessageDecoder;
import com.bomberman.common.message.codec.MessageEncoder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Drives the real TCP client against a scripted server socket speaking the shared codec. */
class ClientNetworkIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExecutorService uiThread = Executors.newSingleThreadExecutor();
    private final BlockingQueue<ScreenId> shownScreens = new LinkedBlockingQueue<>();
    private final BlockingQueue<String> feedback = new LinkedBlockingQueue<>();
    private final ClientState state = new ClientState();
    private final CountDownLatch connectionLost = new CountDownLatch(1);
    private PendingRequests pendingRequests;
    private GameNetworkClient networkClient;
    private GameClientController controller;

    @AfterEach
    void tearDown() {
        if (controller != null) {
            controller.close();
        }
        if (networkClient != null) {
            networkClient.close();
        }
        if (pendingRequests != null) {
            pendingRequests.close();
        }
        uiThread.shutdownNow();
    }

    @Test
    void loginOverTcpOpensHomeAndServerShutdownReturnsToLogin() throws Exception {
        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            CompletableFuture<NetworkMessage> receivedRequest = new CompletableFuture<>();
            CountDownLatch closeConnection = new CountDownLatch(1);
            Thread.ofPlatform().daemon().start(() -> answerOneLogin(server, receivedRequest, closeConnection));
            connectClient(server.getLocalPort());

            CompletableFuture<NetworkMessage> reply = controller.login("Minh Đức", "secret");

            assertEquals(MessageType.LOGIN_RESPONSE, reply.get(5, TimeUnit.SECONDS).type());
            assertEquals(ScreenId.HOME, shownScreens.poll(5, TimeUnit.SECONDS));
            NetworkMessage request = receivedRequest.get(5, TimeUnit.SECONDS);
            assertEquals(MessageType.LOGIN_REQUEST, request.type());
            assertEquals("Minh Đức", objectMapper.treeToValue(request.payload(), LoginRequest.class).username());
            assertEquals("Minh Đức", uiThread.submit(state::getCurrentUsername).get());

            closeConnection.countDown();

            assertEquals(ScreenId.LOGIN, shownScreens.poll(5, TimeUnit.SECONDS));
            assertTrue(connectionLost.await(5, TimeUnit.SECONDS));
            assertEquals(false, uiThread.submit(state::isConnected).get());
        }
    }

    @Test
    void unreachableServerIsReportedWithItsAddress() throws Exception {
        int unusedPort;
        try (ServerSocket probe = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            unusedPort = probe.getLocalPort();
        }
        connectClient(unusedPort);

        CompletableFuture<NetworkMessage> reply = controller.login("alex", "secret");

        assertEquals("Cannot connect to 127.0.0.1:" + unusedPort + ".", feedback.poll(10, TimeUnit.SECONDS));
        assertThrows(java.util.concurrent.ExecutionException.class, () -> reply.get(5, TimeUnit.SECONDS));
    }

    private void connectClient(int port) {
        Navigator navigator = new Navigator() {
            private volatile ScreenId current = ScreenId.LOGIN;

            @Override
            public void show(ScreenId screen) {
                current = screen;
                shownScreens.add(screen);
            }

            @Override
            public ScreenId current() {
                return current;
            }
        };
        state.addFeedbackListener(entry -> feedback.add(entry.message()));
        pendingRequests = new PendingRequests(uiThread, PendingRequests.DEFAULT_TIMEOUT, () -> { });
        networkClient = new GameNetworkClient(new ClientMessageDispatcher(
                state, navigator, uiThread, pendingRequests, connectionLost::countDown));
        controller = new GameClientController(
                networkClient,
                state,
                new ClientNetworkConfig("127.0.0.1", port),
                pendingRequests,
                uiThread
        );
    }

    private void answerOneLogin(
            ServerSocket server,
            CompletableFuture<NetworkMessage> receivedRequest,
            CountDownLatch closeConnection
    ) {
        try (Socket socket = server.accept()) {
            NetworkMessage request = new MessageDecoder().decode(socket.getInputStream());
            receivedRequest.complete(request);
            String username = objectMapper.treeToValue(request.payload(), LoginRequest.class).username();
            LoginResponse response = new LoginResponse(true, AuthResultCode.SUCCESS, 42L, username, PlayerStatus.FREE);
            new MessageEncoder().encode(
                    new NetworkMessage(MessageType.LOGIN_RESPONSE, request.requestId(), objectMapper.valueToTree(response)),
                    socket.getOutputStream()
            );
            closeConnection.await(5, TimeUnit.SECONDS);
        } catch (IOException | InterruptedException exception) {
            receivedRequest.completeExceptionally(exception);
        }
    }
}
