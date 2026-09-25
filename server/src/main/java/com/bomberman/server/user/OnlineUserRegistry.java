package com.bomberman.server.user;

import com.bomberman.common.enums.PlayerStatus;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory presence registry. Absence means the account is offline.
 */
@Component
public class OnlineUserRegistry {

    private final ConcurrentMap<Long, OnlineUserPresence> onlineUsers = new ConcurrentHashMap<>();

    public boolean markOnline(long userId, String username, String sessionId) {
        return onlineUsers.putIfAbsent(
                userId,
                new OnlineUserPresence(userId, username, sessionId, PlayerStatus.FREE)
        ) == null;
    }

    public void markOffline(long userId, String sessionId) {
        onlineUsers.computeIfPresent(userId, (ignored, onlineUser) ->
                onlineUser.sessionId().equals(sessionId) ? null : onlineUser
        );
    }

    public boolean updateStatus(long userId, PlayerStatus status) {
        return onlineUsers.computeIfPresent(userId, (ignored, onlineUser) ->
                new OnlineUserPresence(
                        onlineUser.userId(),
                        onlineUser.username(),
                        onlineUser.sessionId(),
                        status
                )
        ) != null;
    }

    public boolean isOnline(long userId) {
        return onlineUsers.containsKey(userId);
    }

    public Optional<PlayerStatus> findStatus(long userId) {
        return Optional.ofNullable(onlineUsers.get(userId)).map(OnlineUserPresence::status);
    }

    public List<OnlineUserPresence> snapshot() {
        return onlineUsers.values().stream()
                .sorted(Comparator.comparing(OnlineUserPresence::username))
                .toList();
    }

    public record OnlineUserPresence(
            long userId,
            String username,
            String sessionId,
            PlayerStatus status
    ) {
    }
}
