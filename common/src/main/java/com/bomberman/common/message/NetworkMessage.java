package com.bomberman.common.message;

import com.bomberman.common.enums.MessageType;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;

/**
 * JSON envelope transported inside a length-prefixed TCP frame.
 *
 * <p>{@code requestId} correlates a response with a request and may be
 * {@code null} for unsolicited server updates. {@code payload} may be
 * {@code null} for messages which do not carry data.</p>
 */
public record NetworkMessage(MessageType type, String requestId, JsonNode payload) {

    public NetworkMessage {
        Objects.requireNonNull(type, "type must not be null");
        if (payload != null && payload.isNull()) {
            payload = null;
        }
    }

    public static NetworkMessage withoutPayload(MessageType type, String requestId) {
        return new NetworkMessage(type, requestId, null);
    }
}
