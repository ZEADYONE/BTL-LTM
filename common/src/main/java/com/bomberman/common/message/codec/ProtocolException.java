package com.bomberman.common.message.codec;

import java.io.IOException;

/**
 * Indicates that a received or outgoing frame violates the wire protocol.
 */
public class ProtocolException extends IOException {

    public ProtocolException(String message) {
        super(message);
    }

    public ProtocolException(String message, Throwable cause) {
        super(message, cause);
    }
}
