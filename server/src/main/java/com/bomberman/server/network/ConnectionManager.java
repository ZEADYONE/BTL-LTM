package com.bomberman.server.network;

import com.bomberman.common.message.NetworkMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Thread-safe registry of currently connected client sessions.
 */
@Component
public class ConnectionManager {

    private static final Logger log = LoggerFactory.getLogger(ConnectionManager.class);

    private final ConcurrentMap<String, ClientSession> sessions = new ConcurrentHashMap<>();

    public void add(ClientSession session) {
        ClientSession previous = sessions.putIfAbsent(session.getSessionId(), session);
        if (previous != null) {
            throw new IllegalStateException("Duplicate session id: " + session.getSessionId());
        }
    }

    public Optional<ClientSession> find(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    public void remove(String sessionId) {
        sessions.remove(sessionId);
    }

    public int getConnectionCount() {
        return sessions.size();
    }

    public Collection<ClientSession> snapshot() {
        return List.copyOf(sessions.values());
    }

    public void broadcast(NetworkMessage message) {
        for (ClientSession session : snapshot()) {
            try {
                session.send(message);
            } catch (IOException exception) {
                log.info("Broadcast failed for session {}; closing it", session.getSessionId());
                session.close();
            }
        }
    }

    public void closeAll() {
        snapshot().forEach(ClientSession::close);
    }
}
