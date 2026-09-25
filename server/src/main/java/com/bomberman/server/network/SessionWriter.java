package com.bomberman.server.network;

import com.bomberman.common.message.NetworkMessage;
import com.bomberman.common.message.codec.MessageEncoder;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Serializes writes to one client so concurrent sends cannot interleave frames.
 */
public final class SessionWriter {

    private final OutputStream output;
    private final MessageEncoder encoder;
    private final Lock writeLock = new ReentrantLock();

    public SessionWriter(OutputStream output, MessageEncoder encoder) {
        this.output = Objects.requireNonNull(output, "output must not be null");
        this.encoder = Objects.requireNonNull(encoder, "encoder must not be null");
    }

    public void send(NetworkMessage message) throws IOException {
        writeLock.lock();
        try {
            encoder.encode(message, output);
        } finally {
            writeLock.unlock();
        }
    }
}
