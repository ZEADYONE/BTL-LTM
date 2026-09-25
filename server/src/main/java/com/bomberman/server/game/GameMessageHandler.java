package com.bomberman.server.game;

import com.bomberman.common.dto.ErrorResponse;
import com.bomberman.common.dto.MoveRequest;
import com.bomberman.common.enums.GameCommandResultCode;
import com.bomberman.common.enums.MessageType;
import com.bomberman.common.enums.RoomStatus;
import com.bomberman.common.message.NetworkMessage;
import com.bomberman.server.auth.AuthenticatedUser;
import com.bomberman.server.network.ClientSession;
import com.bomberman.server.room.RoomManager;
import com.bomberman.server.room.RoomSnapshot;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

/** Converts gameplay network input into commands without mutating game state. */
@Component
public class GameMessageHandler {

    private final GameSessionManager gameSessionManager;
    private final ObjectMapper objectMapper;
    private final RoomManager roomManager;

    public GameMessageHandler(
            GameSessionManager gameSessionManager,
            ObjectMapper objectMapper,
            RoomManager roomManager
    ) {
        this.gameSessionManager = gameSessionManager;
        this.objectMapper = objectMapper;
        this.roomManager = roomManager;
    }

    public void handleMove(ClientSession session, NetworkMessage message) {
        AuthenticatedUser user = requireUser(session, message.requestId());
        MoveRequest request = readMoveRequest(message);
        if (user == null || request == null || request.direction() == null) {
            if (user != null) {
                sendError(session, message.requestId(), GameCommandResultCode.INVALID_REQUEST);
            }
            return;
        }
        GameCommandResultCode validation = validateGameplaySession(session, user);
        if (validation != null) {
            sendError(session, message.requestId(), validation);
            return;
        }
        handleEnqueueResult(
                session,
                message.requestId(),
                gameSessionManager.enqueueMove(user.userId(), request.direction())
        );
    }

    public void handlePlaceBomb(ClientSession session, NetworkMessage message) {
        AuthenticatedUser user = requireUser(session, message.requestId());
        if (user == null) {
            return;
        }
        GameCommandResultCode validation = validateGameplaySession(session, user);
        if (validation != null) {
            sendError(session, message.requestId(), validation);
            return;
        }
        handleEnqueueResult(
                session,
                message.requestId(),
                gameSessionManager.enqueuePlaceBomb(user.userId())
        );
    }

    private AuthenticatedUser requireUser(ClientSession session, String requestId) {
        AuthenticatedUser user = session.getAuthenticatedUser();
        if (user == null) {
            sendError(session, requestId, GameCommandResultCode.NOT_AUTHENTICATED);
        }
        return user;
    }

    private MoveRequest readMoveRequest(NetworkMessage message) {
        if (message.payload() == null) {
            return null;
        }
        try {
            return objectMapper.treeToValue(message.payload(), MoveRequest.class);
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            return null;
        }
    }

    private GameCommandResultCode validateGameplaySession(
            ClientSession session,
            AuthenticatedUser user
    ) {
        RoomSnapshot room = roomManager.findRoomByUser(user.userId());
        if (room == null || room.players().stream().noneMatch(player ->
                player.userId() == user.userId()
                        && player.sessionId().equals(session.getSessionId())
        )) {
            return GameCommandResultCode.NOT_IN_ROOM;
        }
        if (room.status() != RoomStatus.PLAYING) {
            return GameCommandResultCode.ROOM_NOT_PLAYING;
        }
        Optional<Boolean> alive = gameSessionManager.isPlayerAlive(user.userId());
        if (alive.isEmpty()) {
            return GameCommandResultCode.GAME_NOT_RUNNING;
        }
        if (!alive.get()) {
            return GameCommandResultCode.PLAYER_DEAD;
        }
        return null;
    }

    private void handleEnqueueResult(
            ClientSession session,
            String requestId,
            GameCommandEnqueueResult result
    ) {
        switch (result) {
            case ACCEPTED -> {
                // The authoritative game-state update is emitted by a later network-broadcast stage.
            }
            case GAME_NOT_RUNNING -> sendError(
                    session,
                    requestId,
                    GameCommandResultCode.GAME_NOT_RUNNING
            );
            case QUEUE_FULL -> sendError(
                    session,
                    requestId,
                    GameCommandResultCode.COMMAND_QUEUE_FULL
            );
        }
    }

    private void sendError(
            ClientSession session,
            String requestId,
            GameCommandResultCode result
    ) {
        ErrorResponse error = new ErrorResponse(result.name(), result.name());
        try {
            session.send(new NetworkMessage(
                    MessageType.ERROR,
                    requestId,
                    objectMapper.valueToTree(error)
            ));
        } catch (IOException exception) {
            session.close();
        }
    }
}
