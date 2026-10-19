package com.bomberman.server.social;

import com.bomberman.common.dto.FriendDto;
import com.bomberman.common.dto.FriendListUpdate;
import com.bomberman.common.dto.FriendRequestDto;
import com.bomberman.common.enums.FriendPresenceStatus;
import com.bomberman.common.enums.MessageType;
import com.bomberman.common.enums.PlayerStatus;
import com.bomberman.common.message.NetworkMessage;
import com.bomberman.server.network.ClientSession;
import com.bomberman.server.network.ConnectionManager;
import com.bomberman.server.user.OnlineUserRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;

/** Builds and sends recipient-specific friend snapshots. */
@Component
public class FriendUpdatePublisher {

    private static final Logger log = LoggerFactory.getLogger(FriendUpdatePublisher.class);

    private final FriendService friendService;
    private final OnlineUserRegistry onlineUserRegistry;
    private final ConnectionManager connectionManager;
    private final ObjectMapper objectMapper;

    public FriendUpdatePublisher(
            FriendService friendService,
            OnlineUserRegistry onlineUserRegistry,
            ConnectionManager connectionManager,
            ObjectMapper objectMapper
    ) {
        this.friendService = friendService;
        this.onlineUserRegistry = onlineUserRegistry;
        this.connectionManager = connectionManager;
        this.objectMapper = objectMapper;
    }

    public void sendSnapshot(ClientSession session, String requestId, long userId) {
        FriendSnapshot snapshot = friendService.snapshot(userId);
        FriendListUpdate update = new FriendListUpdate(
                snapshot.friends().stream()
                        .map(entry -> new FriendDto(
                                entry.userId(),
                                entry.username(),
                                presence(entry.userId())
                        ))
                        .sorted(Comparator
                                .comparing((FriendDto friend) -> friend.presence() == FriendPresenceStatus.OFFLINE)
                                .thenComparing(FriendDto::username, String.CASE_INSENSITIVE_ORDER))
                        .toList(),
                snapshot.incomingRequests().stream().map(this::requestDto).toList(),
                snapshot.outgoingRequests().stream().map(this::requestDto).toList()
        );
        send(session, new NetworkMessage(
                MessageType.FRIEND_LIST_UPDATE,
                requestId,
                objectMapper.valueToTree(update)
        ));
    }

    public void publishMutation(
            ClientSession actorSession,
            String requestId,
            long actorUserId,
            long otherUserId
    ) {
        sendSnapshot(actorSession, requestId, actorUserId);
        sendSnapshotIfOnline(otherUserId);
    }

    public void publishPresenceChanged(long userId) {
        publishPresenceChanged(Set.of(userId));
    }

    public void publishPresenceChanged(Collection<Long> changedUserIds) {
        Set<Long> recipients = new LinkedHashSet<>(changedUserIds);
        for (long userId : changedUserIds) {
            recipients.addAll(friendService.acceptedFriendIds(userId));
        }
        recipients.forEach(this::sendSnapshotIfOnline);
    }

    private FriendRequestDto requestDto(FriendEntry entry) {
        return new FriendRequestDto(
                entry.relationshipId(),
                entry.userId(),
                entry.username(),
                presence(entry.userId()),
                entry.requestedAtEpochMillis()
        );
    }

    private FriendPresenceStatus presence(long userId) {
        return onlineUserRegistry.findStatus(userId)
                .map(this::presence)
                .orElse(FriendPresenceStatus.OFFLINE);
    }

    private FriendPresenceStatus presence(PlayerStatus status) {
        return switch (status) {
            case FREE -> FriendPresenceStatus.FREE;
            case IN_ROOM -> FriendPresenceStatus.IN_ROOM;
            case PLAYING -> FriendPresenceStatus.PLAYING;
        };
    }

    private void sendSnapshotIfOnline(long userId) {
        onlineUserRegistry.find(userId)
                .flatMap(presence -> connectionManager.find(presence.sessionId()))
                .ifPresent(session -> sendSnapshot(session, null, userId));
    }

    private void send(ClientSession session, NetworkMessage message) {
        try {
            session.send(message);
        } catch (IOException exception) {
            log.info("Failed to send friend update to session {}; closing it", session.getSessionId());
            session.close();
        }
    }
}
