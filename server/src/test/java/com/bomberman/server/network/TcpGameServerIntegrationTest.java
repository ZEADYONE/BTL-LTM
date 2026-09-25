package com.bomberman.server.network;

import com.bomberman.common.enums.MessageType;
import com.bomberman.common.message.NetworkMessage;
import com.bomberman.common.message.codec.MessageDecoder;
import com.bomberman.common.message.codec.MessageEncoder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "bomberman.tcp.port=0")
class TcpGameServerIntegrationTest {

    private static final int CLIENT_COUNT = 4;

    @Autowired
    private TcpGameServer tcpGameServer;

    @Autowired
    private ConnectionManager connectionManager;

    @Test
    @Timeout(15)
    void fourConcurrentClientsReceivePongAndAreRemovedAfterDisconnect() throws Exception {
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch allConnected = new CountDownLatch(CLIENT_COUNT);
        CountDownLatch sendGate = new CountDownLatch(1);
        List<Future<Void>> clientTasks = new ArrayList<>();

        try (ExecutorService clients = Executors.newFixedThreadPool(CLIENT_COUNT)) {
            for (int clientNumber = 0; clientNumber < CLIENT_COUNT; clientNumber++) {
                int currentClient = clientNumber;
                clientTasks.add(clients.submit(() -> {
                    runPingClient(currentClient, startGate, allConnected, sendGate);
                    return null;
                }));
            }

            startGate.countDown();
            assertTrue(allConnected.await(3, TimeUnit.SECONDS));
            assertTrue(waitUntilConnectionCount(CLIENT_COUNT, Duration.ofSeconds(3)));
            sendGate.countDown();

            for (Future<Void> clientTask : clientTasks) {
                clientTask.get(10, TimeUnit.SECONDS);
            }
        }

        assertTrue(waitUntilDisconnected(Duration.ofSeconds(3)));
        assertEquals(0, connectionManager.getConnectionCount());
    }

    private void runPingClient(
            int clientNumber,
            CountDownLatch startGate,
            CountDownLatch allConnected,
            CountDownLatch sendGate
    ) throws Exception {
        startGate.await();

        try (Socket socket = new Socket()) {
            socket.connect(
                    new InetSocketAddress(InetAddress.getLoopbackAddress(), tcpGameServer.getPort()),
                    2_000
            );
            socket.setSoTimeout(3_000);
            allConnected.countDown();
            assertTrue(allConnected.await(3, TimeUnit.SECONDS));
            assertTrue(sendGate.await(3, TimeUnit.SECONDS));

            String requestId = "ping-" + clientNumber;
            NetworkMessage ping = NetworkMessage.withoutPayload(MessageType.PING, requestId);
            new MessageEncoder().encode(ping, socket.getOutputStream());

            NetworkMessage pong = new MessageDecoder().decode(socket.getInputStream());
            assertEquals(MessageType.PONG, pong.type());
            assertEquals(requestId, pong.requestId());
        }
    }

    private boolean waitUntilDisconnected(Duration timeout) throws InterruptedException {
        return waitUntilConnectionCount(0, timeout);
    }

    private boolean waitUntilConnectionCount(int expectedCount, Duration timeout)
            throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (connectionManager.getConnectionCount() != expectedCount
                && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        return connectionManager.getConnectionCount() == expectedCount;
    }
}
