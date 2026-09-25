package com.bomberman.server.network;

import com.bomberman.common.message.NetworkMessage;
import com.bomberman.common.message.codec.MessageDecoder;
import com.bomberman.common.message.codec.MessageEncoder;
import com.bomberman.common.message.codec.ProtocolException;
import com.bomberman.server.auth.AuthenticatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Owns the lifecycle and protocol streams of one connected TCP client.
 */
public final class ClientSession implements Runnable, Closeable {

    private static final Logger log = LoggerFactory.getLogger(ClientSession.class);

    private final String sessionId = UUID.randomUUID().toString();
    private final Socket socket;
    private final InputStream input;
    private final SessionWriter writer;
    private final MessageDecoder decoder;
    private final MessageDispatcher dispatcher;
    private final ConnectionManager connectionManager;
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicReference<AuthenticatedUser> authenticatedUser = new AtomicReference<>();

    public ClientSession(
            Socket socket,
            MessageEncoder encoder,
            MessageDecoder decoder,
            MessageDispatcher dispatcher,
            ConnectionManager connectionManager
    ) throws IOException {
        this.socket = Objects.requireNonNull(socket, "socket must not be null");
        this.decoder = Objects.requireNonNull(decoder, "decoder must not be null");
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher must not be null");
        this.connectionManager = Objects.requireNonNull(
                connectionManager,
                "connectionManager must not be null"
        );

        socket.setTcpNoDelay(true);
        this.input = socket.getInputStream();
        this.writer = new SessionWriter(socket.getOutputStream(), encoder);
    }

    @Override
    public void run() {
        boolean registered = false;

        try {
            connectionManager.add(this);
            registered = true;
            log.info("Client connected: sessionId={}, remoteAddress={}", sessionId, remoteAddress());

            while (!closed.get()) {
                NetworkMessage message = decoder.decode(input);
                dispatcher.dispatch(this, message);
            }
        } catch (ProtocolException exception) {
            log.warn("Protocol violation from session {}: {}", sessionId, exception.getMessage());
        } catch (SocketException exception) {
            if (!closed.get()) {
                log.info("Connection lost: sessionId={}, reason={}", sessionId, exception.getMessage());
            }
        } catch (IOException exception) {
            if (!closed.get()) {
                log.info("Client disconnected: sessionId={}, reason={}", sessionId, exception.getMessage());
            }
        } catch (RuntimeException exception) {
            log.error("Unexpected session failure: sessionId={}", sessionId, exception);
        } finally {
            close();
            try {
                dispatcher.onDisconnect(this);
            } finally {
                if (registered) {
                    connectionManager.remove(sessionId);
                }
            }
            log.info("Session removed: sessionId={}", sessionId);
        }
    }

    public void send(NetworkMessage message) throws IOException {
        if (closed.get()) {
            throw new SocketException("Session is closed");
        }
        writer.send(message);
    }

    public String getSessionId() {
        return sessionId;
    }

    public boolean isClosed() {
        return closed.get();
    }

    public boolean attachAuthenticatedUser(AuthenticatedUser user) {
        return authenticatedUser.compareAndSet(null, Objects.requireNonNull(user));
    }

    public AuthenticatedUser getAuthenticatedUser() {
        return authenticatedUser.get();
    }

    public AuthenticatedUser detachAuthenticatedUser() {
        return authenticatedUser.getAndSet(null);
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            try {
                socket.close();
            } catch (IOException exception) {
                log.debug("Failed to close socket for session {}", sessionId, exception);
            }
        }
    }

    private String remoteAddress() {
        return String.valueOf(socket.getRemoteSocketAddress());
    }
}
