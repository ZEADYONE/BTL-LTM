package com.bomberman.common.message.codec;

import com.bomberman.common.message.NetworkMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

/**
 * Encodes a message as a four-byte big-endian length followed by UTF-8 JSON.
 */
public final class MessageEncoder {

    public static final int DEFAULT_MAX_PAYLOAD_BYTES = 1024 * 1024;

    private final ObjectMapper objectMapper;
    private final int maxPayloadBytes;

    public MessageEncoder() {
        this(new ObjectMapper(), DEFAULT_MAX_PAYLOAD_BYTES);
    }

    public MessageEncoder(ObjectMapper objectMapper) {
        this(objectMapper, DEFAULT_MAX_PAYLOAD_BYTES);
    }

    public MessageEncoder(ObjectMapper objectMapper, int maxPayloadBytes) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        if (maxPayloadBytes <= 0) {
            throw new IllegalArgumentException("maxPayloadBytes must be positive");
        }
        this.maxPayloadBytes = maxPayloadBytes;
    }

    public byte[] encode(NetworkMessage message) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        encode(message, output);
        return output.toByteArray();
    }

    public void encode(NetworkMessage message, OutputStream output) throws IOException {
        Objects.requireNonNull(message, "message must not be null");
        Objects.requireNonNull(output, "output must not be null");

        byte[] payload;
        try {
            payload = objectMapper.writeValueAsBytes(message);
        } catch (JsonProcessingException exception) {
            throw new ProtocolException("Unable to serialize network message", exception);
        }

        if (payload.length == 0 || payload.length > maxPayloadBytes) {
            throw new ProtocolException(
                    "Encoded payload length must be between 1 and " + maxPayloadBytes
                            + " bytes, but was " + payload.length
            );
        }

        DataOutputStream dataOutput = new DataOutputStream(output);
        dataOutput.writeInt(payload.length);
        dataOutput.write(payload);
        dataOutput.flush();
    }
}
