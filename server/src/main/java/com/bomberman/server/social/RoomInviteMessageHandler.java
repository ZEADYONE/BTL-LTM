package com.bomberman.server.social;

import com.bomberman.common.dto.ErrorResponse;
import com.bomberman.common.dto.RespondRoomInviteRequest;
import com.bomberman.common.dto.RoomInviteDto;
import com.bomberman.common.dto.RoomInviteStatusUpdate;
import com.bomberman.common.dto.SendRoomInviteRequest;
import com.bomberman.common.enums.MessageType;
import com.bomberman.common.enums.PlayerStatus;
import com.bomberman.common.enums.RoomInviteResultCode;
import com.bomberman.common.enums.RoomInviteStatus;
import com.bomberman.common.enums.RoomResultCode;
import com.bomberman.common.enums.RoomStatus;
import com.bomberman.common.message.NetworkMessage;
import com.bomberman.server.auth.AuthenticatedUser;
import com.bomberman.server.lobby.LobbyService;
import com.bomberman.server.network.ClientSession;
import com.bomberman.server.network.ConnectionManager;
import com.bomberman.server.room.RoomManager;
import com.bomberman.server.room.RoomMessageHandler;
import com.bomberman.server.room.RoomOperationResult;
import com.bomberman.server.room.RoomSnapshot;
import com.bomberman.server.user.OnlineUserRegistry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Validates, delivers and consumes ephemeral invitations to waiting rooms. */
@Component
public class RoomInviteMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(RoomInviteMessageHandler.class);

    private final RoomInviteManager inviteManager;
    private final FriendService friendService;
    private final SocialRateLimiter rateLimiter;
    private final RoomManager roomManager;
    private final RoomMessageHandler roomMessageHandler;
    private final OnlineUserRegistry onlineUserRegistry;
    private final ConnectionManager connectionManager;
    private final FriendUpdatePublisher friendUpdatePublisher;
    private final LobbyService lobbyService;
    private final ObjectMapper objectMapper;

    public RoomInviteMessageHandler(
            RoomInviteManager inviteManager,
            FriendService friendService,
            SocialRateLimiter rateLimiter,
            RoomManager roomManager,
            RoomMessageHandler roomMessageHandler,
            OnlineUserRegistry onlineUserRegistry,
            ConnectionManager connectionManager,
            FriendUpdatePublisher friendUpdatePublisher,
            LobbyService lobbyService,
            ObjectMapper objectMapper
    ) {
        this.inviteManager = inviteManager;
        this.friendService = friendService;
        this.rateLimiter = rateLimiter;
        this.roomManager = roomManager;
        this.roomMessageHandler = roomMessageHandler;
        this.onlineUserRegistry = onlineUserRegistry;
        this.connectionManager = connectionManager;
        this.friendUpdatePublisher = friendUpdatePublisher;
        this.lobbyService = lobbyService;
        this.objectMapper = objectMapper;
        inviteManager.setTerminalListener(this::publishTerminalStatus);
    }

    public void handleSend(ClientSession session, NetworkMessage message) {
        AuthenticatedUser inviter = requireUser(session, message.requestId());
        SendRoomInviteRequest request = readPayload(message, SendRoomInviteRequest.class);
        if (inviter == null || request == null || request.friendUserId() <= 0) {
            if (inviter != null) {
                sendError(session, message.requestId(), RoomInviteResultCode.INVALID_REQUEST);
            }
            return;
        }
        long inviteeId = request.friendUserId();
        if (!friendService.areFriends(inviter.userId(), inviteeId)) {
            sendError(session, message.requestId(), RoomInviteResultCode.NOT_FRIENDS);
            return;
        }

        RoomSnapshot room = roomManager.findRoomByUser(inviter.userId());
        if (room == null) {
            sendError(session, message.requestId(), RoomInviteResultCode.INVITER_NOT_IN_ROOM);
            return;
        }
        if (room.status() != RoomStatus.WAITING) {
            sendError(session, message.requestId(), RoomInviteResultCode.ROOM_NOT_WAITING);
            return;
        }
        if (roomManager.isKickedFromRoom(room.roomId(), inviteeId)
                && room.hostUserId() != inviter.userId()) {
            sendError(session, message.requestId(), RoomInviteResultCode.TARGET_KICKED);
            return;
        }

        var inviteePresence = onlineUserRegistry.find(inviteeId).orElse(null);
        if (inviteePresence == null) {
            sendError(session, message.requestId(), RoomInviteResultCode.INVITEE_OFFLINE);
            return;
        }
        if (inviteePresence.status() != PlayerStatus.FREE) {
            sendError(session, message.requestId(), RoomInviteResultCode.INVITEE_BUSY);
            return;
        }
        if (!rateLimiter.allowRoomInvite(inviter.userId(), inviteeId)) {
            sendError(session, message.requestId(), RoomInviteResultCode.RATE_LIMITED);
            return;
        }

        RoomInviteCommandResult result = inviteManager.create(
                room.roomId(),
                room.roomName(),
                inviter.userId(),
                inviter.username(),
                inviteeId
        );
        if (!result.isSuccess()) {
            sendError(session, message.requestId(), result.result());
            return;
        }

        sendStatus(session, message.requestId(), result.invite(), RoomInviteStatus.SENT);
        connectionManager.find(inviteePresence.sessionId()).ifPresent(inviteeSession -> send(
                inviteeSession,
                new NetworkMessage(
                        MessageType.ROOM_INVITE_RECEIVED,
                        null,
                        objectMapper.valueToTree(toDto(result.invite()))
                )
        ));
    }

    public void handleRespond(ClientSession session, NetworkMessage message) {
        AuthenticatedUser invitee = requireUser(session, message.requestId());
        RespondRoomInviteRequest request = readPayload(message, RespondRoomInviteRequest.class);
        if (invitee == null || request == null || request.inviteId() == null) {
            if (invitee != null) {
                sendError(session, message.requestId(), RoomInviteResultCode.INVALID_REQUEST);
            }
            return;
        }

        RoomInviteCommandResult taken = inviteManager.takeForResponse(
                request.inviteId(),
                invitee.userId()
        );
        if (!taken.isSuccess()) {
            sendError(session, message.requestId(), taken.result());
            return;
        }
        RoomInvite invite = taken.invite();
        if (!request.accept()) {
            sendStatus(session, message.requestId(), invite, RoomInviteStatus.DECLINED);
            sendStatusToUser(invite.inviterUserId(), null, invite, RoomInviteStatus.DECLINED);
            return;
        }

        RoomInviteResultCode validation = validateAccept(invite, invitee.userId());
        if (validation != null) {
            sendError(session, message.requestId(), validation);
            publishTerminalStatus(invite, RoomInviteStatus.CANCELLED);
            return;
        }

        RoomOperationResult joined = roomManager.joinRoomByInvite(
                invitee,
                session.getSessionId(),
                invite.roomId(),
                invite.inviterUserId()
        );
        if (!joined.isSuccess()) {
            sendError(session, message.requestId(), mapJoinFailure(joined.result()));
            publishTerminalStatus(invite, RoomInviteStatus.CANCELLED);
            return;
        }

        onlineUserRegistry.updateStatus(invitee.userId(), PlayerStatus.IN_ROOM);
        friendUpdatePublisher.publishPresenceChanged(invitee.userId());
        roomMessageHandler.publishRoomState(joined.room(), session.getSessionId(), message.requestId());
        sendStatusToUser(invite.inviterUserId(), null, invite, RoomInviteStatus.ACCEPTED);
        inviteManager.cancelForInvitee(invitee.userId());
        if (joined.room().players().size() >= joined.room().maxPlayers()) {
            inviteManager.cancelByRoom(joined.room().roomId());
        }
        lobbyService.broadcastLobbyUpdates();
    }

    private RoomInviteResultCode validateAccept(RoomInvite invite, long inviteeUserId) {
        if (!friendService.areFriends(invite.inviterUserId(), inviteeUserId)) {
            return RoomInviteResultCode.NOT_FRIENDS;
        }
        var presence = onlineUserRegistry.find(inviteeUserId).orElse(null);
        if (presence == null) {
            return RoomInviteResultCode.INVITEE_OFFLINE;
        }
        if (presence.status() != PlayerStatus.FREE) {
            return RoomInviteResultCode.INVITEE_BUSY;
        }
        return null;
    }

    private RoomInviteResultCode mapJoinFailure(RoomResultCode result) {
        return switch (result) {
            case ROOM_FULL -> RoomInviteResultCode.ROOM_FULL;
            case ROOM_NOT_WAITING -> RoomInviteResultCode.ROOM_NOT_WAITING;
            case KICKED_FROM_ROOM -> RoomInviteResultCode.TARGET_KICKED;
            case TARGET_NOT_IN_ROOM -> RoomInviteResultCode.INVITER_NOT_IN_ROOM;
            case ALREADY_IN_ROOM -> RoomInviteResultCode.INVITEE_BUSY;
            case ROOM_NOT_FOUND -> RoomInviteResultCode.INVITE_NOT_FOUND;
            default -> RoomInviteResultCode.INVALID_REQUEST;
        };
    }

    private void publishTerminalStatus(RoomInvite invite, RoomInviteStatus status) {
        sendStatusToUser(invite.inviterUserId(), null, invite, status);
        sendStatusToUser(invite.inviteeUserId(), null, invite, status);
    }

    private void sendStatusToUser(
            long userId,
            String requestId,
            RoomInvite invite,
            RoomInviteStatus status
    ) {
        onlineUserRegistry.find(userId)
                .flatMap(presence -> connectionManager.find(presence.sessionId()))
                .ifPresent(session -> sendStatus(session, requestId, invite, status));
    }

    private void sendStatus(
            ClientSession session,
            String requestId,
            RoomInvite invite,
            RoomInviteStatus status
    ) {
        send(session, new NetworkMessage(
                MessageType.ROOM_INVITE_STATUS,
                requestId,
                objectMapper.valueToTree(new RoomInviteStatusUpdate(
                        invite.inviteId().toString(),
                        status
                ))
        ));
    }

    private RoomInviteDto toDto(RoomInvite invite) {
        return new RoomInviteDto(
                invite.inviteId().toString(),
                invite.roomId(),
                invite.roomName(),
                invite.inviterUserId(),
                invite.inviterUsername(),
                invite.expiresAt().toEpochMilli()
        );
    }

    private AuthenticatedUser requireUser(ClientSession session, String requestId) {
        AuthenticatedUser user = session.getAuthenticatedUser();
        if (user == null) {
            sendError(session, requestId, RoomInviteResultCode.NOT_AUTHENTICATED);
        }
        return user;
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

    private void sendError(ClientSession session, String requestId, RoomInviteResultCode result) {
        send(session, new NetworkMessage(
                MessageType.ERROR,
                requestId,
                objectMapper.valueToTree(new ErrorResponse(result.name(), humanReadable(result)))
        ));
    }

    private String humanReadable(RoomInviteResultCode result) {
        return switch (result) {
            case NOT_AUTHENTICATED -> "Login is required";
            case NOT_FRIENDS -> "Only friends can be invited";
            case INVITER_NOT_IN_ROOM -> "The inviter is no longer in that room";
            case ROOM_NOT_WAITING -> "The room is no longer waiting for players";
            case INVITEE_OFFLINE -> "That friend is offline";
            case INVITEE_BUSY -> "That friend is already in a room";
            case DUPLICATE_INVITE -> "An invitation is already pending";
            case INVITE_NOT_FOUND -> "The invitation is no longer available";
            case INVITE_EXPIRED -> "The invitation has expired";
            case INVITE_NOT_RECIPIENT -> "That invitation belongs to another player";
            case ROOM_FULL -> "The room is full";
            case TARGET_KICKED -> "Only the host can invite this player back";
            case RATE_LIMITED -> "Too many invitations. Try again later";
            default -> "Invalid room invitation";
        };
    }

    private void send(ClientSession session, NetworkMessage message) {
        try {
            session.send(message);
        } catch (IOException exception) {
            log.info("Failed to send room invite message to {}; closing it", session.getSessionId());
            session.close();
        }
    }
}
