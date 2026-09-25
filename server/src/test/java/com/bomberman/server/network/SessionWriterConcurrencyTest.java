package com.bomberman.server.network;

import com.bomberman.common.enums.MessageType;
import com.bomberman.common.message.NetworkMessage;
import com.bomberman.common.message.codec.MessageDecoder;
import com.bomberman.common.message.codec.MessageEncoder;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionWriterConcurrencyTest {

    @Test
    void concurrentSendsNeverInterleaveLengthPrefixedFrames() throws Exception {
        int writers = 8;
        int messagesPerWriter = 50;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        SessionWriter sessionWriter = new SessionWriter(output, new MessageEncoder());

        try (var executor = Executors.newFixedThreadPool(writers)) {
            for (int writer = 0; writer < writers; writer++) {
                int writerId = writer;
                executor.submit(() -> {
                    for (int message = 0; message < messagesPerWriter; message++) {
                        try {
                            sessionWriter.send(NetworkMessage.withoutPayload(
                                    MessageType.PONG,
                                    writerId + "-" + message
                            ));
                        } catch (java.io.IOException exception) {
                            throw new IllegalStateException(exception);
                        }
                    }
                });
            }
            executor.shutdown();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }

        MessageDecoder decoder = new MessageDecoder();
        ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
        Set<String> requestIds = new HashSet<>();
        for (int index = 0; index < writers * messagesPerWriter; index++) {
            NetworkMessage decoded = decoder.decode(input);
            assertEquals(MessageType.PONG, decoded.type());
            requestIds.add(decoded.requestId());
        }
        assertEquals(writers * messagesPerWriter, requestIds.size());
        assertEquals(0, input.available());
    }
}
