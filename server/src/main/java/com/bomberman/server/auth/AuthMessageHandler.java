package com.bomberman.server.auth;

import com.bomberman.common.dto.LoginRequest;
import com.bomberman.common.dto.LoginResponse;
import com.bomberman.common.dto.RegisterRequest;
import com.bomberman.common.dto.RegisterResponse;
import com.bomberman.common.enums.AuthResultCode;
import com.bomberman.common.enums.MessageType;
import com.bomberman.common.enums.PlayerStatus;
import com.bomberman.common.message.NetworkMessage;
import com.bomberman.server.lobby.LobbyService;
import com.bomberman.server.network.ClientSession;
import com.bomberman.server.room.RoomMessageHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Adapts authentication protocol messages to the authentication service.
 */
@Component
public class AuthMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(AuthMessageHandler.class);

    private final AuthenticationService authenticationService;
    private final ObjectMapper objectMapper;
    private final LobbyService lobbyService;
    private final RoomMessageHandler roomMessageHandler;

    public AuthMessageHandler(
            AuthenticationService authenticationService,
            ObjectMapper objectMapper,
            LobbyService lobbyService,
            RoomMessageHandler roomMessageHandler
    ) {
        this.authenticationService = authenticationService;
        this.objectMapper = objectMapper;
        this.lobbyService = lobbyService;
        this.roomMessageHandler = roomMessageHandler;
    }

    public void handleRegister(ClientSession session, NetworkMessage message) {
        RegisterRequest request = readPayload(message, RegisterRequest.class);
        if (request == null) {
            sendRegisterResponse(
                    session,
                    message.requestId(),
                    new RegistrationResult(AuthResultCode.INVALID_REQUEST, null, null)
            );
            return;
        }

        RegistrationResult result;
        try {
            result = authenticationService.register(request.username(), request.password());
        } catch (DataIntegrityViolationException exception) {
            result = new RegistrationResult(
                    AuthResultCode.USERNAME_ALREADY_EXISTS,
                    null,
                    normalizedUsername(request.username())
            );
        }
        sendRegisterResponse(session, message.requestId(), result);
    }

    public void handleLogin(ClientSession session, NetworkMessage message) {
        if (session.getAuthenticatedUser() != null) {
            sendLoginResponse(
                    session,
                    message.requestId(),
                    new LoginResult(AuthResultCode.SESSION_ALREADY_AUTHENTICATED, null)
            );
            return;
        }

        LoginRequest request = readPayload(message, LoginRequest.class);
        if (request == null) {
            sendLoginResponse(
                    session,
                    message.requestId(),
                    new LoginResult(AuthResultCode.INVALID_REQUEST, null)
            );
            return;
        }

        LoginResult result = authenticationService.login(
                request.username(),
                request.password(),
                session.getSessionId()
        );
        if (result.isSuccess()
                && !session.attachAuthenticatedUser(result.authenticatedUser())) {
            authenticationService.logout(result.authenticatedUser(), session.getSessionId());
            result = new LoginResult(AuthResultCode.SESSION_ALREADY_AUTHENTICATED, null);
        }
        sendLoginResponse(session, message.requestId(), result);
        if (result.isSuccess()) {
            log.info(
                    "Login successful: userId={}, username={}, sessionId={}",
                    result.authenticatedUser().userId(),
                    result.authenticatedUser().username(),
                    session.getSessionId()
            );
            lobbyService.broadcastLobbyUpdates();
        }
    }

    public void handleLogout(ClientSession session) {
        cleanupSession(session, "logout");
    }

    public void handleDisconnect(ClientSession session) {
        cleanupSession(session, "disconnect");
    }

    private void cleanupSession(ClientSession session, String reason) {
        roomMessageHandler.handleSessionExit(session);
        AuthenticatedUser user = session.detachAuthenticatedUser();
        if (user != null) {
            authenticationService.logout(user, session.getSessionId());
            log.info(
                    "User offline: userId={}, username={}, sessionId={}, reason={}",
                    user.userId(),
                    user.username(),
                    session.getSessionId(),
                    reason
            );
            lobbyService.broadcastLobbyUpdates();
        }
    }

    private void sendRegisterResponse(
            ClientSession session,
            String requestId,
            RegistrationResult result
    ) {
        RegisterResponse response = new RegisterResponse(
                result.isSuccess(),
                result.result(),
                result.userId(),
                result.username()
        );
        send(session, new NetworkMessage(
                MessageType.REGISTER_RESPONSE,
                requestId,
                objectMapper.valueToTree(response)
        ));
    }

    private void sendLoginResponse(ClientSession session, String requestId, LoginResult result) {
        AuthenticatedUser user = result.authenticatedUser();
        LoginResponse response = new LoginResponse(
                result.isSuccess(),
                result.result(),
                user == null ? null : user.userId(),
                user == null ? null : user.username(),
                result.isSuccess() ? PlayerStatus.FREE : null
        );
        send(session, new NetworkMessage(
                MessageType.LOGIN_RESPONSE,
                requestId,
                objectMapper.valueToTree(response)
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
            log.info("Failed to send auth response to session {}; closing it", session.getSessionId());
            session.close();
        }
    }

    private String normalizedUsername(String username) {
        return username == null ? null : username.trim();
    }
}
