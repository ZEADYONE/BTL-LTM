package com.bomberman.server.network;

import com.bomberman.common.enums.MessageType;
import com.bomberman.common.message.NetworkMessage;
import com.bomberman.server.auth.AuthMessageHandler;
import com.bomberman.server.game.GameMessageHandler;
import com.bomberman.server.lobby.LobbyMessageHandler;
import com.bomberman.server.match.HistoryMessageHandler;
import com.bomberman.server.ranking.RankingMessageHandler;
import com.bomberman.server.room.RoomMessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Objects;

/**
 * Routes decoded protocol messages to their server-side handlers.
 */
@Component
public class MessageDispatcher {

    private static final Logger log = LoggerFactory.getLogger(MessageDispatcher.class);

    private final AuthMessageHandler authMessageHandler;
    private final LobbyMessageHandler lobbyMessageHandler;
    private final RoomMessageHandler roomMessageHandler;
    private final GameMessageHandler gameMessageHandler;
    private final RankingMessageHandler rankingMessageHandler;
    private final HistoryMessageHandler historyMessageHandler;

    public MessageDispatcher(
            AuthMessageHandler authMessageHandler,
            LobbyMessageHandler lobbyMessageHandler,
            RoomMessageHandler roomMessageHandler,
            GameMessageHandler gameMessageHandler,
            RankingMessageHandler rankingMessageHandler,
            HistoryMessageHandler historyMessageHandler
    ) {
        this.authMessageHandler = authMessageHandler;
        this.lobbyMessageHandler = lobbyMessageHandler;
        this.roomMessageHandler = roomMessageHandler;
        this.gameMessageHandler = gameMessageHandler;
        this.rankingMessageHandler = rankingMessageHandler;
        this.historyMessageHandler = historyMessageHandler;
    }

    public void dispatch(ClientSession session, NetworkMessage message) {
        Objects.requireNonNull(session, "session must not be null");
        Objects.requireNonNull(message, "message must not be null");

        switch (message.type()) {
            case PING -> replyWithPong(session, message);
            case REGISTER_REQUEST -> authMessageHandler.handleRegister(session, message);
            case LOGIN_REQUEST -> authMessageHandler.handleLogin(session, message);
            case LOGOUT -> authMessageHandler.handleLogout(session);
            case ONLINE_USERS_REQUEST -> lobbyMessageHandler.handleOnlineUsersRequest(session, message);
            case ROOM_LIST_REQUEST -> lobbyMessageHandler.handleRoomListRequest(session, message);
            case CREATE_ROOM -> roomMessageHandler.handleCreateRoom(session, message);
            case JOIN_ROOM -> roomMessageHandler.handleJoinRoom(session, message);
            case LEAVE_ROOM -> roomMessageHandler.handleLeaveRoom(session, message);
            case READY -> roomMessageHandler.handleReady(session, message);
            case START_GAME -> roomMessageHandler.handleStartGame(session, message);
            case MOVE -> gameMessageHandler.handleMove(session, message);
            case PLACE_BOMB -> gameMessageHandler.handlePlaceBomb(session, message);
            case PLAY_AGAIN -> roomMessageHandler.handlePlayAgain(session, message);
            case RANKING_REQUEST -> rankingMessageHandler.handleRequest(session, message);
            case HISTORY_REQUEST -> historyMessageHandler.handleRequest(session, message);
            default -> log.debug(
                    "No handler registered for message type {} from session {}",
                    message.type(),
                    session.getSessionId()
            );
        }
    }

    public void onDisconnect(ClientSession session) {
        authMessageHandler.handleDisconnect(session);
    }

    private void replyWithPong(ClientSession session, NetworkMessage ping) {
        NetworkMessage pong = NetworkMessage.withoutPayload(MessageType.PONG, ping.requestId());
        try {
            session.send(pong);
        } catch (IOException exception) {
            log.info("Failed to send PONG to session {}; closing it", session.getSessionId());
            session.close();
        }
    }
}
