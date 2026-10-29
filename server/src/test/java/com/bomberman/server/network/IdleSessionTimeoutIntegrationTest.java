package com.bomberman.server.network;

import com.bomberman.common.dto.LoginRequest;
import com.bomberman.common.dto.LoginResponse;
import com.bomberman.common.dto.RegisterRequest;
import com.bomberman.common.enums.MessageType;
import com.bomberman.common.message.NetworkMessage;
import com.bomberman.common.message.codec.MessageDecoder;
import com.bomberman.common.message.codec.MessageEncoder;
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

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A client that goes silent without closing its socket (e.g. its network changed) must not keep
 * its account online: the server drops it after the idle timeout.
 */
@SpringBootTest(properties = {"bomberman.tcp.port=0", "bomberman.tcp.idle-timeout-ms=500"})
@Timeout(20)
class IdleSessionTimeoutIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private TcpGameServer tcpGameServer;

    @Autowired
    private OnlineUserRegistry onlineUserRegistry;

    @Test
    void silentClientIsDisconnectedAndMarkedOffline() throws Exception {
        String username = "idle_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String password = "correct-password";

        try (Socket silentClient = connect()) {
            exchange(silentClient, MessageType.REGISTER_REQUEST, new RegisterRequest(username, password));
            NetworkMessage reply = exchange(
                    silentClient,
                    MessageType.LOGIN_REQUEST,
                    new LoginRequest(username, password)
            );
            LoginResponse login = objectMapper.treeToValue(reply.payload(), LoginResponse.class);
            assertTrue(login.success());

            assertTrue(waitUntilOffline(login.userId(), Duration.ofSeconds(5)));
        }
    }

    @Test
    void pingingClientStaysConnectedPastIdleTimeout() throws Exception {
        try (Socket client = connect()) {
            for (int beat = 0; beat < 5; beat++) {
                Thread.sleep(200);
                exchange(client, MessageType.PING, null);
            }
        }
    }

    private Socket connect() throws IOException {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), tcpGameServer.getPort()), 2_000);
        socket.setSoTimeout(5_000);
        return socket;
    }

    private NetworkMessage exchange(Socket socket, MessageType type, Object payload) throws IOException {
        String requestId = UUID.randomUUID().toString();
        new MessageEncoder(objectMapper).encode(
                new NetworkMessage(type, requestId, payload == null ? null : objectMapper.valueToTree(payload)),
                socket.getOutputStream()
        );
        MessageDecoder decoder = new MessageDecoder(objectMapper);
        for (int attempt = 0; attempt < 20; attempt++) {
            NetworkMessage message = decoder.decode(socket.getInputStream());
            if (requestId.equals(message.requestId())) {
                return message;
            }
        }
        throw new IOException("No reply to " + type);
    }

    private boolean waitUntilOffline(long userId, Duration timeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (onlineUserRegistry.isOnline(userId) && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        return !onlineUserRegistry.isOnline(userId);
    }
}
