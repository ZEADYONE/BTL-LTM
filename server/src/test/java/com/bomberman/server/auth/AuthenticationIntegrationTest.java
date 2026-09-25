package com.bomberman.server.auth;

import com.bomberman.common.dto.LoginRequest;
import com.bomberman.common.dto.LoginResponse;
import com.bomberman.common.dto.RegisterRequest;
import com.bomberman.common.dto.RegisterResponse;
import com.bomberman.common.enums.AuthResultCode;
import com.bomberman.common.enums.MessageType;
import com.bomberman.common.enums.PlayerStatus;
import com.bomberman.common.message.NetworkMessage;
import com.bomberman.common.message.codec.MessageDecoder;
import com.bomberman.common.message.codec.MessageEncoder;
import com.bomberman.server.network.TcpGameServer;
import com.bomberman.server.repository.UserRepository;
import com.bomberman.server.user.OnlineUserRegistry;
import com.bomberman.server.user.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "bomberman.tcp.port=0")
@Timeout(20)
class AuthenticationIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private TcpGameServer tcpGameServer;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private OnlineUserRegistry onlineUserRegistry;

    @Test
    void registerSucceedsAndStoresOnlyPasswordHash() throws Exception {
        String username = uniqueUsername();
        String password = "correct-password";

        try (TestTcpClient client = connect()) {
            RegisterResponse response = client.register(username, password);

            assertTrue(response.success());
            assertEquals(AuthResultCode.SUCCESS, response.result());
            assertNotNull(response.userId());
            assertEquals(username, response.username());

            User savedUser = userRepository.findById(response.userId()).orElseThrow();
            assertNotEquals(password, savedUser.getPasswordHash());
            assertTrue(passwordEncoder.matches(password, savedUser.getPasswordHash()));
        }
    }

    @Test
    void duplicateUsernameIsRejected() throws Exception {
        String username = uniqueUsername();

        try (TestTcpClient client = connect()) {
            assertTrue(client.register(username, "password-1").success());

            RegisterResponse duplicate = client.register(username, "password-2");

            assertFalse(duplicate.success());
            assertEquals(AuthResultCode.USERNAME_ALREADY_EXISTS, duplicate.result());
        }
    }

    @Test
    void loginSucceedsAndMarksUserFree() throws Exception {
        String username = uniqueUsername();
        String password = "correct-password";

        try (TestTcpClient client = connect()) {
            RegisterResponse registered = client.register(username, password);
            LoginResponse login = client.login(username, password);

            assertTrue(login.success());
            assertEquals(AuthResultCode.SUCCESS, login.result());
            assertEquals(registered.userId(), login.userId());
            assertEquals(PlayerStatus.FREE, login.status());
            assertTrue(onlineUserRegistry.isOnline(login.userId()));
        }
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        String username = uniqueUsername();

        try (TestTcpClient client = connect()) {
            client.register(username, "correct-password");

            LoginResponse response = client.login(username, "wrong-password");

            assertFalse(response.success());
            assertEquals(AuthResultCode.INVALID_CREDENTIALS, response.result());
        }
    }

    @Test
    void accountCannotLoginFromTwoClientsAtOnce() throws Exception {
        String username = uniqueUsername();
        String password = "correct-password";

        try (TestTcpClient firstClient = connect(); TestTcpClient secondClient = connect()) {
            firstClient.register(username, password);
            assertTrue(firstClient.login(username, password).success());

            LoginResponse duplicateLogin = secondClient.login(username, password);

            assertFalse(duplicateLogin.success());
            assertEquals(AuthResultCode.ACCOUNT_ALREADY_ONLINE, duplicateLogin.result());
        }
    }

    @Test
    void disconnectMarksUserOfflineAndAllowsLoginAgain() throws Exception {
        String username = uniqueUsername();
        String password = "correct-password";
        long userId;

        TestTcpClient firstClient = connect();
        try {
            firstClient.register(username, password);
            LoginResponse login = firstClient.login(username, password);
            userId = login.userId();
            assertTrue(onlineUserRegistry.isOnline(userId));
        } finally {
            firstClient.close();
        }

        assertTrue(waitUntilOffline(userId, Duration.ofSeconds(3)));

        try (TestTcpClient secondClient = connect()) {
            assertTrue(secondClient.login(username, password).success());
        }
    }

    @Test
    void logoutMarksUserOfflineWithoutClosingTcpConnection() throws Exception {
        String username = uniqueUsername();
        String password = "correct-password";

        try (TestTcpClient client = connect()) {
            client.register(username, password);
            LoginResponse login = client.login(username, password);

            client.logout();

            assertTrue(waitUntilOffline(login.userId(), Duration.ofSeconds(3)));
            assertTrue(client.ping());
            assertTrue(client.login(username, password).success());
        }
    }

    @Test
    void blankUsernameAndPasswordAreRejected() throws Exception {
        try (TestTcpClient client = connect()) {
            RegisterResponse blankUsername = client.register("   ", "password");
            RegisterResponse blankPassword = client.register(uniqueUsername(), "  ");

            assertEquals(AuthResultCode.INVALID_USERNAME, blankUsername.result());
            assertEquals(AuthResultCode.INVALID_PASSWORD, blankPassword.result());
        }
    }

    private TestTcpClient connect() throws IOException {
        return new TestTcpClient(tcpGameServer.getPort());
    }

    private boolean waitUntilOffline(long userId, Duration timeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (onlineUserRegistry.isOnline(userId) && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        return !onlineUserRegistry.isOnline(userId);
    }

    private String uniqueUsername() {
        return "user_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private final class TestTcpClient implements AutoCloseable {

        private final Socket socket = new Socket();
        private final MessageEncoder encoder = new MessageEncoder(objectMapper);
        private final MessageDecoder decoder = new MessageDecoder(objectMapper);

        private TestTcpClient(int port) throws IOException {
            socket.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 2_000);
            socket.setSoTimeout(5_000);
        }

        private RegisterResponse register(String username, String password) throws IOException {
            return exchange(
                    MessageType.REGISTER_REQUEST,
                    MessageType.REGISTER_RESPONSE,
                    new RegisterRequest(username, password),
                    RegisterResponse.class
            );
        }

        private LoginResponse login(String username, String password) throws IOException {
            return exchange(
                    MessageType.LOGIN_REQUEST,
                    MessageType.LOGIN_RESPONSE,
                    new LoginRequest(username, password),
                    LoginResponse.class
            );
        }

        private void logout() throws IOException {
            encoder.encode(
                    NetworkMessage.withoutPayload(MessageType.LOGOUT, UUID.randomUUID().toString()),
                    socket.getOutputStream()
            );
        }

        private boolean ping() throws IOException {
            String requestId = UUID.randomUUID().toString();
            encoder.encode(
                    NetworkMessage.withoutPayload(MessageType.PING, requestId),
                    socket.getOutputStream()
            );
            NetworkMessage response = readMatching(MessageType.PONG, requestId);
            return response.type() == MessageType.PONG && requestId.equals(response.requestId());
        }

        private <T> T exchange(
                MessageType requestType,
                MessageType responseType,
                Object payload,
                Class<T> responseClass
        ) throws IOException {
            String requestId = UUID.randomUUID().toString();
            encoder.encode(
                    new NetworkMessage(requestType, requestId, objectMapper.valueToTree(payload)),
                    socket.getOutputStream()
            );

            NetworkMessage response = readMatching(responseType, requestId);
            assertEquals(responseType, response.type());
            assertEquals(requestId, response.requestId());
            return objectMapper.treeToValue(response.payload(), responseClass);
        }

        private NetworkMessage readMatching(MessageType type, String requestId) throws IOException {
            for (int attempt = 0; attempt < 20; attempt++) {
                NetworkMessage message = decoder.decode(socket.getInputStream());
                if (message.type() == type && requestId.equals(message.requestId())) {
                    return message;
                }
            }
            throw new IOException("Expected response was not received: " + type);
        }

        @Override
        public void close() throws IOException {
            socket.close();
        }
    }
}
