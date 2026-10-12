package com.bomberman.clientfx.network;

import com.bomberman.clientfx.state.ClientState;
import com.bomberman.clientfx.ui.Navigator;
import com.bomberman.clientfx.ui.ScreenId;
import com.bomberman.common.dto.ErrorResponse;
import com.bomberman.common.dto.GameStateDto;
import com.bomberman.common.dto.FriendDto;
import com.bomberman.common.dto.FriendListUpdate;
import com.bomberman.common.dto.FriendRequestDto;
import com.bomberman.common.dto.LoginResponse;
import com.bomberman.common.dto.OnlineUserDto;
import com.bomberman.common.dto.OnlineUsersUpdate;
import com.bomberman.common.dto.RegisterResponse;
import com.bomberman.common.dto.RoomMemberRemovedNotice;
import com.bomberman.common.dto.RoomPlayerDto;
import com.bomberman.common.dto.RoomStateDto;
import com.bomberman.common.dto.RoomStateUpdate;
import com.bomberman.common.dto.RoomInviteDto;
import com.bomberman.common.dto.RoomInviteStatusUpdate;
import com.bomberman.common.enums.AuthResultCode;
import com.bomberman.common.enums.GameStatus;
import com.bomberman.common.enums.FriendPresenceStatus;
import com.bomberman.common.enums.MessageType;
import com.bomberman.common.enums.PlayerStatus;
import com.bomberman.common.enums.RoomRemovalReason;
import com.bomberman.common.enums.RoomStatus;
import com.bomberman.common.enums.RoomInviteStatus;
import com.bomberman.common.enums.TileType;
import com.bomberman.common.message.NetworkMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientMessageDispatcherTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ClientState state = new ClientState();
    private final RecordingNavigator navigator = new RecordingNavigator();
    private final List<String> feedback = new ArrayList<>();
    private final PendingRequests pendingRequests = new PendingRequests(Runnable::run, java.time.Duration.ofSeconds(5), () -> { });
    private int connectionLostCount;
    private ClientMessageDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        state.addFeedbackListener(entry -> feedback.add(entry.message()));
        dispatcher = new ClientMessageDispatcher(state, navigator, Runnable::run, pendingRequests,
                () -> connectionLostCount++);
        navigator.show(ScreenId.LOGIN);
    }

    @Test
    void successfulLoginStoresUserAndOpensHome() {
        dispatcher.onMessage(message(MessageType.LOGIN_RESPONSE,
                new LoginResponse(true, AuthResultCode.SUCCESS, 7L, "Minh Đức", PlayerStatus.FREE)));

        assertTrue(state.isLoggedIn());
        assertEquals(7L, state.getCurrentUserId());
        assertEquals("Minh Đức", state.getCurrentUsername());
        assertEquals(ScreenId.HOME, navigator.current());
    }

    @Test
    void failedLoginExplainsTheReasonAndStaysOnLogin() {
        dispatcher.onMessage(message(MessageType.LOGIN_RESPONSE,
                new LoginResponse(false, AuthResultCode.INVALID_CREDENTIALS, null, null, null)));

        assertFalse(state.isLoggedIn());
        assertEquals(List.of("Wrong username or password."), feedback);
        assertEquals(ScreenId.LOGIN, navigator.current());
    }

    @Test
    void registrationResultIsAnnounced() {
        dispatcher.onMessage(message(MessageType.REGISTER_RESPONSE,
                new RegisterResponse(true, AuthResultCode.SUCCESS, 3L, "alex")));
        dispatcher.onMessage(message(MessageType.REGISTER_RESPONSE,
                new RegisterResponse(false, AuthResultCode.USERNAME_ALREADY_EXISTS, null, null)));

        assertEquals(List.of("Account created. You can log in now.", "That username is already taken."), feedback);
    }

    @Test
    void onlineUsersUpdateReplacesTheList() {
        dispatcher.onMessage(message(MessageType.ONLINE_USERS_UPDATE,
                new OnlineUsersUpdate(List.of(new OnlineUserDto(1L, "alex", PlayerStatus.FREE)))));

        assertEquals(1, state.getOnlineUsers().size());
    }

    @Test
    void friendListUpdateReplacesStateAndOnlyNewPushRequestsAnnounce() {
        FriendListUpdate first = friends(
                List.of(new FriendDto(2, "alice", FriendPresenceStatus.FREE)),
                List.of(new FriendRequestDto(
                        11,
                        3,
                        "bob",
                        FriendPresenceStatus.OFFLINE,
                        1_760_000_000_000L
                ))
        );

        dispatcher.onMessage(message(MessageType.FRIEND_LIST_UPDATE, first));
        dispatcher.onMessage(message(MessageType.FRIEND_LIST_UPDATE, first));

        assertEquals(1, state.getFriends().size());
        assertEquals(1, state.getIncomingFriendRequests().size());
        assertEquals(List.of("Friend request from bob"), feedback);

        FriendListUpdate correlated = friends(
                first.friends(),
                List.of(
                        first.incomingRequests().getFirst(),
                        new FriendRequestDto(
                                12,
                                4,
                                "carol",
                                FriendPresenceStatus.FREE,
                                1_760_000_001_000L
                        )
                )
        );
        dispatcher.onMessage(new NetworkMessage(
                MessageType.FRIEND_LIST_UPDATE,
                "friends-1",
                objectMapper.valueToTree(correlated)
        ));

        assertEquals(2, state.getIncomingFriendRequests().size());
        assertEquals(List.of("Friend request from bob"), feedback);
    }

    @Test
    void friendReplyCompletesAfterSocialStateIsApplied() {
        CompletableFuture<NetworkMessage> reply = pendingRequests.register("friends-1", false);
        reply.thenRun(() -> assertEquals(1, state.getFriends().size()));
        FriendListUpdate update = friends(
                List.of(new FriendDto(2, "alice", FriendPresenceStatus.FREE)),
                List.of()
        );

        dispatcher.onMessage(new NetworkMessage(
                MessageType.FRIEND_LIST_UPDATE,
                "friends-1",
                objectMapper.valueToTree(update)
        ));

        assertTrue(reply.isDone());
    }

    @Test
    void roomStateRoutesByMembershipAndStatus() {
        dispatcher.onMessage(roomState(true, RoomStatus.WAITING));
        assertEquals(ScreenId.ROOM_LOBBY, navigator.current());
        assertNotNull(state.getRoom());

        dispatcher.onMessage(roomState(true, RoomStatus.PLAYING));
        assertEquals(ScreenId.GAME, navigator.current());

        dispatcher.onMessage(roomState(false, RoomStatus.WAITING));
        assertEquals(ScreenId.HOME, navigator.current());
        assertNull(state.getRoom());
    }

    @Test
    void roomMemberRemovedNoticeExplainsWhoKickedThePlayer() {
        dispatcher.onMessage(message(
                MessageType.ROOM_MEMBER_REMOVED,
                new RoomMemberRemovedNotice(
                        "room-1",
                        "Bomber Party!",
                        RoomRemovalReason.HOST_KICK,
                        1,
                        "alex"
                )
        ));

        assertEquals(List.of("Host alex removed you from Bomber Party!"), feedback);
    }

    @Test
    void incomingInviteIsStoredAndTerminalStatusRemovesIt() {
        RoomInviteDto invite = new RoomInviteDto(
                "invite-1", "room-1", "Bomber Party!", 2, "alice", Long.MAX_VALUE
        );
        dispatcher.onMessage(message(MessageType.ROOM_INVITE_RECEIVED, invite));
        assertEquals(List.of(invite), state.getPendingRoomInvites());

        dispatcher.onMessage(message(
                MessageType.ROOM_INVITE_STATUS,
                new RoomInviteStatusUpdate("invite-1", RoomInviteStatus.EXPIRED)
        ));

        assertTrue(state.getPendingRoomInvites().isEmpty());
        assertEquals(List.of("Room invitation expired."), feedback);
    }

    @Test
    void correlatedSentStatusCompletesRequestWithoutToast() {
        CompletableFuture<NetworkMessage> reply = pendingRequests.register("invite-send", false);
        dispatcher.onMessage(new NetworkMessage(
                MessageType.ROOM_INVITE_STATUS,
                "invite-send",
                objectMapper.valueToTree(new RoomInviteStatusUpdate("invite-1", RoomInviteStatus.SENT))
        ));

        assertTrue(reply.isDone());
        assertTrue(feedback.isEmpty());
    }

    @Test
    void rematchStartedByAnotherPlayerKeepsTheResultScreen() {
        navigator.show(ScreenId.RESULT);

        dispatcher.onMessage(roomState(true, RoomStatus.WAITING));

        assertEquals(ScreenId.RESULT, navigator.current());
        assertEquals(RoomStatus.WAITING, state.getRoom().status());
    }

    @Test
    void gameStateIsStoredAndOpensTheGameScreen() {
        dispatcher.onMessage(roomState(true, RoomStatus.PLAYING));
        navigator.show(ScreenId.ROOM_LOBBY);

        dispatcher.onMessage(message(MessageType.GAME_STATE, snapshot(4)));

        assertEquals(4, state.getGameState().tick());
        assertEquals(ScreenId.GAME, navigator.current());
    }

    @Test
    void runningSnapshotThatOvertakesThePlayingRoomStateIsPreserved() {
        dispatcher.onMessage(message(MessageType.GAME_STATE, snapshot(2)));
        assertEquals(2, state.getGameState().tick());

        dispatcher.onMessage(roomState(true, RoomStatus.PLAYING));

        assertEquals(2, state.getGameState().tick());
        assertEquals(ScreenId.GAME, navigator.current());
    }

    @Test
    void lateSnapshotsAfterLeavingDoNotPullThePlayerBackIntoTheMatch() {
        dispatcher.onMessage(roomState(true, RoomStatus.PLAYING));
        dispatcher.onMessage(roomState(false, RoomStatus.PLAYING));
        assertEquals(ScreenId.HOME, navigator.current());

        dispatcher.onMessage(message(MessageType.GAME_STATE, snapshot(9)));

        assertEquals(ScreenId.HOME, navigator.current());
    }

    private static GameStateDto snapshot(long tick) {
        return new GameStateDto(tick, GameStatus.RUNNING, List.of(List.of(TileType.EMPTY)), List.of(), List.of(), List.of(), 2);
    }

    @Test
    void gameCommandErrorsAreSilentDuringTheMatchOnly() {
        NetworkMessage error = message(MessageType.ERROR, new ErrorResponse("PLAYER_DEAD", "Player is dead"));

        navigator.show(ScreenId.GAME);
        dispatcher.onMessage(error);
        assertTrue(feedback.isEmpty());

        navigator.show(ScreenId.HOME);
        dispatcher.onMessage(error);
        assertEquals(List.of("Player is dead"), feedback);
    }

    @Test
    void disconnectLogsOutAndReturnsToLogin() {
        state.login(1L, "alex");
        navigator.show(ScreenId.HOME);

        state.setConnected(true);
        CompletableFuture<NetworkMessage> waiting = pendingRequests.register("request-1", false);

        dispatcher.onDisconnected();

        assertFalse(state.isLoggedIn());
        assertFalse(state.isConnected());
        assertEquals(ScreenId.LOGIN, navigator.current());
        assertEquals(1, connectionLostCount);
        assertTrue(waiting.isCompletedExceptionally());
    }

    @Test
    void repliesCompleteTheirPendingRequestAfterBeingApplied() {
        CompletableFuture<NetworkMessage> reply = pendingRequests.register("login-1", false);
        reply.thenRun(() -> assertTrue(state.isLoggedIn(), "state must be updated before the caller hears back"));

        dispatcher.onMessage(new NetworkMessage(MessageType.LOGIN_RESPONSE, "login-1", objectMapper.valueToTree(
                new LoginResponse(true, AuthResultCode.SUCCESS, 7L, "alex", PlayerStatus.FREE))));

        assertTrue(reply.isDone());
        assertEquals(MessageType.LOGIN_RESPONSE, reply.join().type());
    }

    @Test
    void quietRequestsHandleTheirOwnErrors() {
        CompletableFuture<NetworkMessage> reply = pendingRequests.register("join-1", true);

        dispatcher.onMessage(new NetworkMessage(MessageType.ERROR, "join-1",
                objectMapper.valueToTree(new ErrorResponse("ROOM_FULL", "Room is full"))));

        assertTrue(feedback.isEmpty());
        assertEquals("ROOM_FULL", reply.join().payload().path("code").asText());
    }

    private NetworkMessage roomState(boolean member, RoomStatus status) {
        RoomStateDto room = new RoomStateDto(
                "room-1", "Bomber Party!", 1L, List.of(new RoomPlayerDto(1L, "alex", false)), 4, status
        );
        return message(MessageType.ROOM_STATE, new RoomStateUpdate(member, room));
    }

    private NetworkMessage message(MessageType type, Object payload) {
        return new NetworkMessage(type, null, objectMapper.valueToTree(payload));
    }

    private FriendListUpdate friends(
            List<FriendDto> friends,
            List<FriendRequestDto> incoming
    ) {
        return new FriendListUpdate(friends, incoming, List.of());
    }

    private static final class RecordingNavigator implements Navigator {

        private ScreenId current;

        @Override
        public void show(ScreenId screen) {
            current = screen;
        }

        @Override
        public ScreenId current() {
            return current;
        }
    }
}
