package com.bomberman.clientfx.network;

import com.bomberman.common.message.NetworkMessage;
import com.bomberman.common.message.codec.MessageDecoder;
import com.bomberman.common.message.codec.MessageEncoder;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/** Persistent TCP client whose blocking read loop runs on a virtual thread. */
public final class GameNetworkClient implements Closeable {

    private static final int CONNECT_TIMEOUT_MILLIS = 3_000;

    private final ServerListener listener;
    private final MessageEncoder encoder = new MessageEncoder();
    private final MessageDecoder decoder = new MessageDecoder();
    private final Lock writeLock = new ReentrantLock();
    private final AtomicBoolean connected = new AtomicBoolean();

    private volatile Socket socket;

    public GameNetworkClient(ServerListener listener) {
        this.listener = Objects.requireNonNull(listener, "listener must not be null");
    }

    public synchronized void connect(String host, int port) throws IOException {
        if (connected.get()) {
            return;
        }

        Socket newSocket = new Socket();
        try {
            newSocket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MILLIS);
            newSocket.setTcpNoDelay(true);
            socket = newSocket;
            connected.set(true);
            Thread.ofVirtual().name("server-listener").start(() -> readMessages(newSocket));
        } catch (IOException exception) {
            newSocket.close();
            throw exception;
        }
    }

    public void send(NetworkMessage message) throws IOException {
        writeLock.lock();
        try {
            Socket currentSocket = socket;
            if (!connected.get() || currentSocket == null || currentSocket.isClosed()) {
                throw new SocketException("Not connected to server");
            }
            encoder.encode(message, currentSocket.getOutputStream());
        } finally {
            writeLock.unlock();
        }
    }

    public boolean isConnected() {
        return connected.get();
    }

    @Override
    public synchronized void close() {
        connected.set(false);
        Socket currentSocket = socket;
        socket = null;
        if (currentSocket != null) {
            try {
                currentSocket.close();
            } catch (IOException ignored) {
                // Connection is already being discarded.
            }
        }
    }

    private void readMessages(Socket connectionSocket) {
        try {
            while (connected.get() && socket == connectionSocket) {
                listener.onMessage(decoder.decode(connectionSocket.getInputStream()));
            }
        } catch (IOException exception) {
            if (connected.get() && socket == connectionSocket) {
                listener.onError(exception);
            }
        } finally {
            boolean wasConnected = clearConnection(connectionSocket);
            if (wasConnected) {
                listener.onDisconnected();
            }
        }
    }

    private synchronized boolean clearConnection(Socket connectionSocket) {
        if (socket != connectionSocket) {
            closeQuietly(connectionSocket);
            return false;
        }
        socket = null;
        boolean wasConnected = connected.getAndSet(false);
        closeQuietly(connectionSocket);
        return wasConnected;
    }

    private void closeQuietly(Socket connectionSocket) {
        if (connectionSocket == null) {
            return;
        }
        try {
            connectionSocket.close();
        } catch (IOException ignored) {
            // Socket is already unusable.
        }
    }
}
