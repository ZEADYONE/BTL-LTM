package com.bomberman.server.lobby;

import com.bomberman.common.dto.OnlineUserDto;
import com.bomberman.common.dto.OnlineUsersUpdate;
import com.bomberman.common.dto.RoomListUpdate;
import com.bomberman.common.enums.MessageType;
import com.bomberman.common.enums.PlayerStatus;
import com.bomberman.common.message.NetworkMessage;
import com.bomberman.server.network.ClientSession;
import com.bomberman.server.network.ConnectionManager;
import com.bomberman.server.room.RoomDtoMapper;
import com.bomberman.server.room.RoomManager;
import com.bomberman.server.user.OnlineUserRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;

/** Builds and publishes authoritative lobby snapshots. */
@Service
public class LobbyService {

    private static final Logger log = LoggerFactory.getLogger(LobbyService.class);

    private final OnlineUserRegistry onlineUserRegistry;
    private final RoomManager roomManager;
    private final RoomDtoMapper roomDtoMapper;
    private final ConnectionManager connectionManager;
    private final ObjectMapper objectMapper;

    public LobbyService(
            OnlineUserRegistry onlineUserRegistry,
            RoomManager roomManager,
            RoomDtoMapper roomDtoMapper,
            ConnectionManager connectionManager,
            ObjectMapper objectMapper
    ) {
        this.onlineUserRegistry = onlineUserRegistry;
        this.roomManager = roomManager;
        this.roomDtoMapper = roomDtoMapper;
        this.connectionManager = connectionManager;
        this.objectMapper = objectMapper;
    }

    public void sendOnlineUsers(ClientSession session, String requestId) {
        send(session, onlineUsersMessage(requestId));
    }

    public void sendRoomList(ClientSession session, String requestId) {
        send(session, roomListMessage(requestId));
    }

    public void broadcastLobbyUpdates() {
        NetworkMessage onlineUsers = onlineUsersMessage(null);
        NetworkMessage roomList = roomListMessage(null);

        for (ClientSession session : connectionManager.snapshot()) {
            if (isInLobby(session)) {
                send(session, onlineUsers);
                send(session, roomList);
            }
        }
    }

    private boolean isInLobby(ClientSession session) {
        if (session.getAuthenticatedUser() == null) {
            return false;
        }
        return onlineUserRegistry.findStatus(session.getAuthenticatedUser().userId())
                .filter(status -> status == PlayerStatus.FREE)
                .isPresent();
    }

    private NetworkMessage onlineUsersMessage(String requestId) {
        OnlineUsersUpdate update = new OnlineUsersUpdate(
                onlineUserRegistry.snapshot().stream()
                        .map(user -> new OnlineUserDto(
                                user.userId(),
                                user.username(),
                                user.status()
                        ))
                        .toList()
        );
        return new NetworkMessage(
                MessageType.ONLINE_USERS_UPDATE,
                requestId,
                objectMapper.valueToTree(update)
        );
    }

    private NetworkMessage roomListMessage(String requestId) {
        RoomListUpdate update = new RoomListUpdate(
                roomManager.snapshotRooms().stream()
                        .map(roomDtoMapper::toSummary)
                        .toList()
        );
        return new NetworkMessage(
                MessageType.ROOM_LIST_UPDATE,
                requestId,
                objectMapper.valueToTree(update)
        );
    }

    private void send(ClientSession session, NetworkMessage message) {
        try {
            session.send(message);
        } catch (IOException exception) {
            log.info("Failed to send lobby update to session {}; closing it", session.getSessionId());
            session.close();
        }
    }
}
