package com.bomberman.server.lobby;

import com.bomberman.common.dto.ErrorResponse;
import com.bomberman.common.enums.MessageType;
import com.bomberman.common.enums.RoomResultCode;
import com.bomberman.common.message.NetworkMessage;
import com.bomberman.server.network.ClientSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class LobbyMessageHandler {

    private final LobbyService lobbyService;
    private final ObjectMapper objectMapper;

    public LobbyMessageHandler(LobbyService lobbyService, ObjectMapper objectMapper) {
        this.lobbyService = lobbyService;
        this.objectMapper = objectMapper;
    }

    public void handleOnlineUsersRequest(ClientSession session, NetworkMessage message) {
        if (!isAuthenticated(session, message.requestId())) {
            return;
        }
        lobbyService.sendOnlineUsers(session, message.requestId());
    }

    public void handleRoomListRequest(ClientSession session, NetworkMessage message) {
        if (!isAuthenticated(session, message.requestId())) {
            return;
        }
        lobbyService.sendRoomList(session, message.requestId());
    }

    private boolean isAuthenticated(ClientSession session, String requestId) {
        if (session.getAuthenticatedUser() != null) {
            return true;
        }

        ErrorResponse error = new ErrorResponse(
                RoomResultCode.NOT_AUTHENTICATED.name(),
                "Login is required"
        );
        try {
            session.send(new NetworkMessage(
                    MessageType.ERROR,
                    requestId,
                    objectMapper.valueToTree(error)
            ));
        } catch (IOException exception) {
            session.close();
        }
        return false;
    }
}
