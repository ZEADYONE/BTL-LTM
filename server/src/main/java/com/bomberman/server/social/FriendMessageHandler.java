package com.bomberman.server.social;

import com.bomberman.common.dto.CancelFriendRequest;
import com.bomberman.common.dto.ErrorResponse;
import com.bomberman.common.dto.RemoveFriendRequest;
import com.bomberman.common.dto.RespondFriendRequest;
import com.bomberman.common.dto.SendFriendRequest;
import com.bomberman.common.enums.FriendResultCode;
import com.bomberman.common.enums.MessageType;
import com.bomberman.common.message.NetworkMessage;
import com.bomberman.server.auth.AuthenticatedUser;
import com.bomberman.server.network.ClientSession;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Handles authenticated friend commands and publishes complete snapshots. */
@Component
public class FriendMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(FriendMessageHandler.class);

    private final FriendService friendService;
    private final FriendUpdatePublisher publisher;
    private final ObjectMapper objectMapper;

    public FriendMessageHandler(
            FriendService friendService,
            FriendUpdatePublisher publisher,
            ObjectMapper objectMapper
    ) {
        this.friendService = friendService;
        this.publisher = publisher;
        this.objectMapper = objectMapper;
    }

    public void handleList(ClientSession session, NetworkMessage message) {
        AuthenticatedUser user = requireUser(session, message.requestId());
        if (user != null) {
            publisher.sendSnapshot(session, message.requestId(), user.userId());
        }
    }

    public void handleSendRequest(ClientSession session, NetworkMessage message) {
        AuthenticatedUser user = requireUser(session, message.requestId());
        SendFriendRequest request = readPayload(message, SendFriendRequest.class);
        if (user == null || request == null) {
            invalidIfAuthenticated(session, message.requestId(), user);
            return;
        }
        publishResult(
                session,
                message.requestId(),
                user.userId(),
                friendService.sendRequest(user.userId(), request.username())
        );
    }

    public void handleRespond(ClientSession session, NetworkMessage message) {
        AuthenticatedUser user = requireUser(session, message.requestId());
        RespondFriendRequest request = readPayload(message, RespondFriendRequest.class);
        if (user == null || request == null || request.relationshipId() <= 0) {
            invalidIfAuthenticated(session, message.requestId(), user);
            return;
        }
        publishResult(
                session,
                message.requestId(),
                user.userId(),
                friendService.respond(user.userId(), request.relationshipId(), request.accept())
        );
    }

    public void handleCancel(ClientSession session, NetworkMessage message) {
        AuthenticatedUser user = requireUser(session, message.requestId());
        CancelFriendRequest request = readPayload(message, CancelFriendRequest.class);
        if (user == null || request == null || request.relationshipId() <= 0) {
            invalidIfAuthenticated(session, message.requestId(), user);
            return;
        }
        publishResult(
                session,
                message.requestId(),
                user.userId(),
                friendService.cancel(user.userId(), request.relationshipId())
        );
    }

    public void handleRemove(ClientSession session, NetworkMessage message) {
        AuthenticatedUser user = requireUser(session, message.requestId());
        RemoveFriendRequest request = readPayload(message, RemoveFriendRequest.class);
        if (user == null || request == null || request.friendUserId() <= 0) {
            invalidIfAuthenticated(session, message.requestId(), user);
            return;
        }
        publishResult(
                session,
                message.requestId(),
                user.userId(),
                friendService.remove(user.userId(), request.friendUserId())
        );
    }

    private void publishResult(
            ClientSession session,
            String requestId,
            long actorUserId,
            FriendCommandResult result
    ) {
        if (!result.isSuccess()) {
            sendError(session, requestId, result.error());
            return;
        }
        publisher.publishMutation(session, requestId, actorUserId, result.otherUserId());
    }

    private AuthenticatedUser requireUser(ClientSession session, String requestId) {
        AuthenticatedUser user = session.getAuthenticatedUser();
        if (user == null) {
            sendError(session, requestId, FriendResultCode.NOT_AUTHENTICATED);
        }
        return user;
    }

    private void invalidIfAuthenticated(
            ClientSession session,
            String requestId,
            AuthenticatedUser user
    ) {
        if (user != null) {
            sendError(session, requestId, FriendResultCode.INVALID_REQUEST);
        }
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

    private void sendError(ClientSession session, String requestId, FriendResultCode result) {
        ErrorResponse error = new ErrorResponse(result.name(), humanReadable(result));
        try {
            session.send(new NetworkMessage(
                    MessageType.ERROR,
                    requestId,
                    objectMapper.valueToTree(error)
            ));
        } catch (IOException exception) {
            log.info("Failed to send friend error to session {}; closing it", session.getSessionId());
            session.close();
        }
    }

    private String humanReadable(FriendResultCode result) {
        return switch (result) {
            case NOT_AUTHENTICATED -> "Login is required";
            case USER_NOT_FOUND -> "Player was not found";
            case CANNOT_ADD_SELF -> "You cannot add yourself";
            case ALREADY_FRIENDS -> "You are already friends";
            case REQUEST_ALREADY_PENDING -> "A friend request is already pending";
            case REQUEST_NOT_FOUND -> "Friend request was not found";
            case NOT_REQUEST_RECIPIENT -> "Only the recipient can respond to this request";
            case NOT_REQUEST_SENDER -> "Only the sender can cancel this request";
            case NOT_FRIENDS -> "The players are not friends";
            case FRIEND_LIMIT_REACHED -> "Friend limit reached";
            case PENDING_LIMIT_REACHED -> "Pending friend request limit reached";
            case RATE_LIMITED -> "Too many friend requests. Please try again later";
            default -> "Invalid friend request";
        };
    }
}
