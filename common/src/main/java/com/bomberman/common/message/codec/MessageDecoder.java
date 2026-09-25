package com.bomberman.common.message.codec;

import com.bomberman.common.message.NetworkMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * Reads exactly one length-prefixed message from a stream.
 *
 * <p>{@link DataInputStream#readFully(byte[])} makes decoding independent of
 * TCP packet boundaries. Calling {@link #decode(InputStream)} repeatedly on
 * the same stream reads consecutive frames without consuming bytes from the
 * following frame.</p>
 */
public final class MessageDecoder {

    private final ObjectReader messageReader;
    private final int maxPayloadBytes;

    public MessageDecoder() {
        this(new ObjectMapper(), MessageEncoder.DEFAULT_MAX_PAYLOAD_BYTES);
    }

    public MessageDecoder(ObjectMapper objectMapper) {
        this(objectMapper, MessageEncoder.DEFAULT_MAX_PAYLOAD_BYTES);
    }

    public MessageDecoder(ObjectMapper objectMapper, int maxPayloadBytes) {
        Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        if (maxPayloadBytes <= 0) {
            throw new IllegalArgumentException("maxPayloadBytes must be positive");
        }
        this.messageReader = objectMapper.readerFor(NetworkMessage.class)
                .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
        this.maxPayloadBytes = maxPayloadBytes;
    }

    public NetworkMessage decode(InputStream input) throws IOException {
        Objects.requireNonNull(input, "input must not be null");

        DataInputStream dataInput = new DataInputStream(input);
        int payloadLength = dataInput.readInt();
        validatePayloadLength(payloadLength);

        byte[] payload = new byte[payloadLength];
        dataInput.readFully(payload);

        try {
            return messageReader.readValue(payload);
        } catch (JsonProcessingException exception) {
            throw new ProtocolException("Malformed JSON payload", exception);
        }
    }

    private void validatePayloadLength(int payloadLength) throws ProtocolException {
        if (payloadLength <= 0 || payloadLength > maxPayloadBytes) {
            throw new ProtocolException(
                    "Payload length must be between 1 and " + maxPayloadBytes
                            + " bytes, but was " + payloadLength
            );
        }
    }
}
