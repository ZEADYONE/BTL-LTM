package com.bomberman.common.message.codec;

import com.bomberman.common.enums.MessageType;
import com.bomberman.common.message.NetworkMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MessageCodecTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MessageEncoder encoder = new MessageEncoder(objectMapper);
    private final MessageDecoder decoder = new MessageDecoder(objectMapper);

    @Test
    void encodesAndDecodesMessage() throws IOException {
        ObjectNode payload = objectMapper.createObjectNode()
                .put("username", "player1")
                .put("password", "secret");
        NetworkMessage expected = new NetworkMessage(MessageType.LOGIN_REQUEST, "request-1", payload);

        NetworkMessage actual = decoder.decode(new ByteArrayInputStream(encoder.encode(expected)));

        assertEquals(expected, actual);
    }

    @Test
    void decodesMultipleConsecutiveMessages() throws IOException {
        NetworkMessage first = NetworkMessage.withoutPayload(MessageType.PING, "ping-1");
        NetworkMessage second = NetworkMessage.withoutPayload(MessageType.PONG, "ping-1");
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        encoder.encode(first, stream);
        encoder.encode(second, stream);

        ByteArrayInputStream input = new ByteArrayInputStream(stream.toByteArray());

        assertEquals(first, decoder.decode(input));
        assertEquals(second, decoder.decode(input));
        assertEquals(0, input.available());
    }

    @Test
    void preservesUnicodeContent() throws IOException {
        JsonNode payload = objectMapper.createObjectNode()
                .put("displayName", "Người chơi số 1 💣");
        NetworkMessage expected = new NetworkMessage(MessageType.REGISTER_REQUEST, "đăng-ký-1", payload);

        NetworkMessage actual = decoder.decode(new ByteArrayInputStream(encoder.encode(expected)));

        assertEquals(expected, actual);
    }

    @Test
    void preservesNestedJsonPayload() throws IOException {
        JsonNode payload = objectMapper.readTree("""
                {
                  "room": {"id": 42, "players": ["alice", "bob"]},
                  "ready": true
                }
                """);
        NetworkMessage expected = new NetworkMessage(MessageType.ROOM_STATE, null, payload);

        NetworkMessage actual = decoder.decode(new ByteArrayInputStream(encoder.encode(expected)));

        assertEquals(expected, actual);
    }

    @Test
    void handlesPartialReads() throws IOException {
        NetworkMessage expected = NetworkMessage.withoutPayload(MessageType.ROOM_LIST_REQUEST, "request-2");
        InputStream oneByteAtATime = new OneByteAtATimeInputStream(
                new ByteArrayInputStream(encoder.encode(expected))
        );

        assertEquals(expected, decoder.decode(oneByteAtATime));
    }

    @Test
    void rejectsMalformedJsonPayload() {
        byte[] malformedJson = "{not-json".getBytes(StandardCharsets.UTF_8);
        ByteBuffer frame = ByteBuffer.allocate(Integer.BYTES + malformedJson.length)
                .putInt(malformedJson.length)
                .put(malformedJson);

        assertThrows(
                ProtocolException.class,
                () -> decoder.decode(new ByteArrayInputStream(frame.array()))
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, MessageEncoder.DEFAULT_MAX_PAYLOAD_BYTES + 1})
    void rejectsInvalidPayloadLength(int invalidLength) {
        byte[] frame = ByteBuffer.allocate(Integer.BYTES).putInt(invalidLength).array();

        assertThrows(
                ProtocolException.class,
                () -> decoder.decode(new ByteArrayInputStream(frame))
        );
    }

    private static final class OneByteAtATimeInputStream extends FilterInputStream {

        private OneByteAtATimeInputStream(InputStream input) {
            super(input);
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            return super.read(buffer, offset, Math.min(length, 1));
        }
    }
}
