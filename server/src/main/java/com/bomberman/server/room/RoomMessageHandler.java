package com.bomberman.server.room;

import com.bomberman.common.dto.CreateRoomRequest;
import com.bomberman.common.dto.ErrorResponse;
import com.bomberman.common.dto.JoinRoomRequest;
import com.bomberman.common.dto.ReadyRequest;
import com.bomberman.common.dto.RoomStateDto;
import com.bomberman.common.dto.RoomStateUpdate;
import com.bomberman.common.enums.MessageType;
import com.bomberman.common.enums.PlayerStatus;
import com.bomberman.common.enums.RoomResultCode;
import com.bomberman.common.enums.RoomStatus;
import com.bomberman.common.message.NetworkMessage;
import com.bomberman.server.auth.AuthenticatedUser;
import com.bomberman.server.game.GameSessionManager;
import com.bomberman.server.lobby.LobbyService;
import com.bomberman.server.network.ClientSession;
import com.bomberman.server.network.ConnectionManager;
import com.bomberman.server.user.OnlineUserRegistry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Handles room commands and publishes authoritative room snapshots. */
@Component
public class RoomMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(RoomMessageHandler.class);

    private final RoomManager roomManager;
    private final RoomDtoMapper roomDtoMapper;
    private final OnlineUserRegistry onlineUserRegistry;
    private final ConnectionManager connectionManager;
    private final LobbyService lobbyService;
    private final ObjectMapper objectMapper;
    private final GameSessionManager gameSessionManager;

    public RoomMessageHandler(
            RoomManager roomManager,
            RoomDtoMapper roomDtoMapper,
            OnlineUserRegistry onlineUserRegistry,
            ConnectionManager connectionManager,
            LobbyService lobbyService,
            ObjectMapper objectMapper,
            GameSessionManager gameSessionManager
    ) {
        this.roomManager = roomManager;
        this.roomDtoMapper = roomDtoMapper;
        this.onlineUserRegistry = onlineUserRegistry;
        this.connectionManager = connectionManager;
        this.lobbyService = lobbyService;
        this.objectMapper = objectMapper;
        this.gameSessionManager = gameSessionManager;
    }

    public void handleCreateRoom(ClientSession session, NetworkMessage message) {
        AuthenticatedUser user = requireUser(session, message.requestId());
        CreateRoomRequest request = readPayload(message, CreateRoomRequest.class);
        if (user == null || request == null) {
            if (user != null) {
                sendError(session, message.requestId(), RoomResultCode.INVALID_REQUEST);
            }
            return;
        }

        RoomOperationResult result = roomManager.createRoom(
                user,
                session.getSessionId(),
                request.roomName()
        );
        if (!result.isSuccess()) {
            sendError(session, message.requestId(), result.result());
            return;
        }

        onlineUserRegistry.updateStatus(user.userId(), PlayerStatus.IN_ROOM);
        log.info(
                "Room created: roomId={}, roomName={}, hostUserId={}",
                result.room().roomId(),
                result.room().roomName(),
                user.userId()
        );
        publishRoomState(result.room(), session.getSessionId(), message.requestId());
        lobbyService.broadcastLobbyUpdates();
    }

    public void handleJoinRoom(ClientSession session, NetworkMessage message) {
        AuthenticatedUser user = requireUser(session, message.requestId());
        JoinRoomRequest request = readPayload(message, JoinRoomRequest.class);
        if (user == null || request == null || request.roomId() == null || request.roomId().isBlank()) {
            if (user != null) {
                sendError(session, message.requestId(), RoomResultCode.INVALID_REQUEST);
            }
            return;
        }

        RoomOperationResult result = roomManager.joinRoom(
                user,
                session.getSessionId(),
                request.roomId()
        );
        if (!result.isSuccess()) {
            sendError(session, message.requestId(), result.result());
            return;
        }

        onlineUserRegistry.updateStatus(user.userId(), PlayerStatus.IN_ROOM);
        log.info("Room joined: roomId={}, userId={}", result.room().roomId(), user.userId());
        publishRoomState(result.room(), session.getSessionId(), message.requestId());
        lobbyService.broadcastLobbyUpdates();
    }

    public void handleLeaveRoom(ClientSession session, NetworkMessage message) {
        AuthenticatedUser user = requireUser(session, message.requestId());
        if (user == null) {
            return;
        }

        RoomSnapshot currentRoom = roomManager.findRoomByUser(user.userId());
        if (currentRoom != null && currentRoom.status() == RoomStatus.PLAYING) {
            gameSessionManager.enqueueDisconnect(user.userId());
        }
        RoomOperationResult result = roomManager.leaveRoom(user.userId());
        if (!result.isSuccess()) {
            sendError(session, message.requestId(), result.result());
            return;
        }

        onlineUserRegistry.updateStatus(user.userId(), PlayerStatus.FREE);
        sendRoomState(session, message.requestId(), false, result.room());
        publishRoomState(result.room(), null, null);
        lobbyService.broadcastLobbyUpdates();
    }

    public void handleReady(ClientSession session, NetworkMessage message) {
        AuthenticatedUser user = requireUser(session, message.requestId());
        ReadyRequest request = readPayload(message, ReadyRequest.class);
        if (user == null || request == null) {
            if (user != null) {
                sendError(session, message.requestId(), RoomResultCode.INVALID_REQUEST);
            }
            return;
        }

        RoomOperationResult result = roomManager.setReady(user.userId(), request.ready());
        if (!result.isSuccess()) {
            sendError(session, message.requestId(), result.result());
            return;
        }
        publishRoomState(result.room(), session.getSessionId(), message.requestId());
    }

    public void handleStartGame(ClientSession session, NetworkMessage message) {
        AuthenticatedUser user = requireUser(session, message.requestId());
        if (user == null) {
            return;
        }

        RoomOperationResult result = roomManager.startGame(user.userId());
        if (!result.isSuccess()) {
            sendError(session, message.requestId(), result.result());
            return;
        }

        result.room().players().forEach(player ->
                onlineUserRegistry.updateStatus(player.userId(), PlayerStatus.PLAYING)
        );
        gameSessionManager.startGame(result.room());
        log.info(
                "Game started: roomId={}, hostUserId={}, players={}",
                result.room().roomId(),
                user.userId(),
                result.room().players().size()
        );
        publishRoomState(result.room(), session.getSessionId(), message.requestId());
        lobbyService.broadcastLobbyUpdates();
    }

    public void handlePlayAgain(ClientSession session, NetworkMessage message) {
        AuthenticatedUser user = requireUser(session, message.requestId());
        if (user == null) {
            return;
        }
        RoomOperationResult result = roomManager.prepareRematch(user.userId());
        if (!result.isSuccess()) {
            sendError(session, message.requestId(), result.result());
            return;
        }
        log.info("Room prepared for rematch: roomId={}, userId={}",
                result.room().roomId(), user.userId());
        publishRoomState(result.room(), session.getSessionId(), message.requestId());
        lobbyService.broadcastLobbyUpdates();
    }

    public void handleSessionExit(ClientSession session) {
        AuthenticatedUser user = session.getAuthenticatedUser();
        if (user == null) {
            return;
        }

        RoomSnapshot currentRoom = roomManager.findRoomByUser(user.userId());
        if (currentRoom != null && currentRoom.status() == RoomStatus.PLAYING) {
            gameSessionManager.enqueueDisconnect(user.userId());
        }
        RoomOperationResult result = roomManager.leaveRoom(user.userId());
        if (result.isSuccess()) {
            log.info(
                    "Session left room: userId={}, roomId={}, roomStatus={}",
                    user.userId(),
                    currentRoom == null ? null : currentRoom.roomId(),
                    currentRoom == null ? null : currentRoom.status()
            );
            publishRoomState(result.room(), null, null);
        }
    }

    private AuthenticatedUser requireUser(ClientSession session, String requestId) {
        AuthenticatedUser user = session.getAuthenticatedUser();
        if (user == null) {
            sendError(session, requestId, RoomResultCode.NOT_AUTHENTICATED);
        }
        return user;
    }

    private void publishRoomState(
            RoomSnapshot room,
            String requestingSessionId,
            String requestId
    ) {
        if (room == null) {
            return;
        }
        for (RoomPlayerSnapshot player : room.players()) {
            connectionManager.find(player.sessionId()).ifPresent(session -> sendRoomState(
                    session,
                    player.sessionId().equals(requestingSessionId) ? requestId : null,
                    true,
                    room
            ));
        }
    }

    private void sendRoomState(
            ClientSession session,
            String requestId,
            boolean member,
            RoomSnapshot room
    ) {
        RoomStateDto roomState = room == null ? null : roomDtoMapper.toState(room);
        RoomStateUpdate update = new RoomStateUpdate(member, roomState);
        send(session, new NetworkMessage(
                MessageType.ROOM_STATE,
                requestId,
                objectMapper.valueToTree(update)
        ));
    }

    private void sendError(ClientSession session, String requestId, RoomResultCode result) {
        ErrorResponse error = new ErrorResponse(result.name(), humanReadable(result));
        send(session, new NetworkMessage(
                MessageType.ERROR,
                requestId,
                objectMapper.valueToTree(error)
        ));
    }

    private <T> T readPayload(NetworkMessage message, Class<T> payloadType) {
        if (message.payload() == null) {
            return null;
        }
        try {
            return objectMapper.treeToValue(message.payload(), payloadType);
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            log.debug("Invalid {} payload", message.type(), exception);
            return null;
        }
    }

    private void send(ClientSession session, NetworkMessage message) {
        try {
            session.send(message);
        } catch (IOException exception) {
            log.info("Failed to send room message to session {}; closing it", session.getSessionId());
            session.close();
        }
    }

    private String humanReadable(RoomResultCode result) {
        return switch (result) {
            case NOT_AUTHENTICATED -> "Login is required";
            case INVALID_ROOM_NAME -> "Room name is required";
            case ALREADY_IN_ROOM -> "User is already in a room";
            case ROOM_NOT_FOUND -> "Room was not found";
            case ROOM_NOT_WAITING -> "Room is not waiting for players";
            case ROOM_NOT_FINISHED -> "The current game has not finished";
            case ROOM_FULL -> "Room is full";
            case NOT_IN_ROOM -> "User is not in a room";
            case NOT_HOST -> "Only the host can start the game";
            case NOT_ENOUGH_PLAYERS -> "At least two players are required";
            case NOT_ALL_PLAYERS_READY -> "All players must be ready";
            default -> "Invalid room request";
        };
    }
}
