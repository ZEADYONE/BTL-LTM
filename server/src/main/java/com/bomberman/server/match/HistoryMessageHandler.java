package com.bomberman.server.match;

import com.bomberman.common.dto.ErrorResponse;
import com.bomberman.common.enums.MessageType;
import com.bomberman.common.message.NetworkMessage;
import com.bomberman.server.network.ClientSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class HistoryMessageHandler {

    private final MatchHistoryService historyService;
    private final ObjectMapper objectMapper;

    public HistoryMessageHandler(MatchHistoryService historyService, ObjectMapper objectMapper) {
        this.historyService = historyService;
        this.objectMapper = objectMapper;
    }

    public void handleRequest(ClientSession session, NetworkMessage request) {
        if (session.getAuthenticatedUser() == null) {
            send(session, new NetworkMessage(
                    MessageType.ERROR,
                    request.requestId(),
                    objectMapper.valueToTree(new ErrorResponse(
                            "NOT_AUTHENTICATED",
                            "Login is required"
                    ))
            ));
            return;
        }
        send(session, new NetworkMessage(
                MessageType.HISTORY_RESPONSE,
                request.requestId(),
                objectMapper.valueToTree(historyService.getHistory(
                        session.getAuthenticatedUser().userId()
                ))
        ));
    }

    private void send(ClientSession session, NetworkMessage message) {
        try {
            session.send(message);
        } catch (IOException exception) {
            session.close();
        }
    }
}
