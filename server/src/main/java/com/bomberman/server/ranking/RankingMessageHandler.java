package com.bomberman.server.ranking;

import com.bomberman.common.dto.ErrorResponse;
import com.bomberman.common.enums.MessageType;
import com.bomberman.common.message.NetworkMessage;
import com.bomberman.server.network.ClientSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RankingMessageHandler {

    private final RankingService rankingService;
    private final ObjectMapper objectMapper;

    public RankingMessageHandler(RankingService rankingService, ObjectMapper objectMapper) {
        this.rankingService = rankingService;
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
                MessageType.RANKING_RESPONSE,
                request.requestId(),
                objectMapper.valueToTree(rankingService.getRanking())
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
