package com.bomberman.server.network;

import com.bomberman.common.message.codec.MessageDecoder;
import com.bomberman.common.message.codec.MessageEncoder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Spring-managed TCP endpoint for persistent gameplay connections.
 */
@Component
public class TcpGameServer implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(TcpGameServer.class);
    private static final int SHUTDOWN_TIMEOUT_SECONDS = 5;

    private final int configuredPort;
    private final ConnectionManager connectionManager;
    private final MessageDispatcher dispatcher;
    private final MessageEncoder encoder;
    private final MessageDecoder decoder;
    private final AtomicBoolean running = new AtomicBoolean();

    private volatile ServerSocket serverSocket;
    private volatile ExecutorService acceptorExecutor;
    private volatile ExecutorService sessionExecutor;

    public TcpGameServer(
            @Value("${bomberman.tcp.port}") int configuredPort,
            ConnectionManager connectionManager,
            MessageDispatcher dispatcher,
            ObjectMapper objectMapper
    ) {
        if (configuredPort < 0 || configuredPort > 65_535) {
            throw new IllegalArgumentException("TCP port must be between 0 and 65535");
        }
        this.configuredPort = configuredPort;
        this.connectionManager = connectionManager;
        this.dispatcher = dispatcher;
        this.encoder = new MessageEncoder(objectMapper);
        this.decoder = new MessageDecoder(objectMapper);
    }

    @Override
    public synchronized void start() {
        if (running.get()) {
            return;
        }

        try {
            ServerSocket socket = new ServerSocket();
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(configuredPort));
            serverSocket = socket;

            acceptorExecutor = Executors.newSingleThreadExecutor(
                    Thread.ofPlatform().name("tcp-game-acceptor").factory()
            );
            sessionExecutor = Executors.newVirtualThreadPerTaskExecutor();
            running.set(true);
            acceptorExecutor.execute(this::acceptConnections);
            log.info("TCP game server listening on port {}", getPort());
        } catch (IOException exception) {
            closeServerSocket();
            throw new IllegalStateException(
                    "Unable to bind TCP game server to port " + configuredPort,
                    exception
            );
        }
    }

    @Override
    public synchronized void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }

        closeServerSocket();
        connectionManager.closeAll();
        shutdownExecutor(acceptorExecutor);
        shutdownExecutor(sessionExecutor);
        log.info("TCP game server stopped");
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    public int getPort() {
        ServerSocket socket = serverSocket;
        return socket == null ? configuredPort : socket.getLocalPort();
    }

    private void acceptConnections() {
        while (running.get()) {
            try {
                ServerSocket listeningSocket = serverSocket;
                if (listeningSocket == null) {
                    break;
                }
                Socket clientSocket = listeningSocket.accept();
                submitSession(clientSocket);
            } catch (SocketException exception) {
                if (running.get()) {
                    log.error("TCP accept loop stopped unexpectedly", exception);
                }
            } catch (IOException exception) {
                if (running.get()) {
                    log.warn("Failed to accept TCP connection", exception);
                }
            }
        }
    }

    private void submitSession(Socket clientSocket) {
        try {
            ClientSession session = new ClientSession(
                    clientSocket,
                    encoder,
                    decoder,
                    dispatcher,
                    connectionManager
            );
            sessionExecutor.execute(session);
        } catch (IOException | RuntimeException exception) {
            log.warn("Unable to initialize client session", exception);
            closeSocket(clientSocket);
        }
    }

    private void closeServerSocket() {
        ServerSocket socket = serverSocket;
        serverSocket = null;
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException exception) {
                log.debug("Failed to close TCP server socket", exception);
            }
        }
    }

    private void shutdownExecutor(ExecutorService executor) {
        if (executor == null) {
            return;
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException exception) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void closeSocket(Socket socket) {
        try {
            socket.close();
        } catch (IOException exception) {
            log.debug("Failed to close uninitialized client socket", exception);
        }
    }
}
