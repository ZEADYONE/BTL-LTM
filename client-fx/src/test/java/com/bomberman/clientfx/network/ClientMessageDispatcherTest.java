package com.bomberman.clientfx.network;

import com.bomberman.clientfx.state.ClientState;
import com.bomberman.clientfx.ui.Navigator;
import com.bomberman.clientfx.ui.ScreenId;
import com.bomberman.common.dto.ErrorResponse;
import com.bomberman.common.dto.GameStateDto;
import com.bomberman.common.dto.LoginResponse;
import com.bomberman.common.dto.OnlineUserDto;
import com.bomberman.common.dto.OnlineUsersUpdate;
import com.bomberman.common.dto.RegisterResponse;
import com.bomberman.common.dto.RoomPlayerDto;
import com.bomberman.common.dto.RoomStateDto;
import com.bomberman.common.dto.RoomStateUpdate;
import com.bomberman.common.enums.AuthResultCode;
import com.bomberman.common.enums.GameStatus;
import com.bomberman.common.enums.MessageType;
import com.bomberman.common.enums.PlayerStatus;
import com.bomberman.common.enums.RoomStatus;
import com.bomberman.common.enums.TileType;
import com.bomberman.common.message.NetworkMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
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
    private ClientMessageDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        state.addFeedbackListener(feedback::add);
        dispatcher = new ClientMessageDispatcher(state, navigator, Runnable::run);
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
    void rematchStartedByAnotherPlayerKeepsTheResultScreen() {
        navigator.show(ScreenId.RESULT);

        dispatcher.onMessage(roomState(true, RoomStatus.WAITING));

        assertEquals(ScreenId.RESULT, navigator.current());
        assertEquals(RoomStatus.WAITING, state.getRoom().status());
    }

    @Test
    void gameStateIsStoredAndOpensTheGameScreen() {
        GameStateDto snapshot = new GameStateDto(
                4, GameStatus.RUNNING, List.of(List.of(TileType.EMPTY)), List.of(), List.of(), List.of(), 2
        );

        dispatcher.onMessage(message(MessageType.GAME_STATE, snapshot));

        assertEquals(4, state.getGameState().tick());
        assertEquals(ScreenId.GAME, navigator.current());
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

        dispatcher.onDisconnected();

        assertFalse(state.isLoggedIn());
        assertEquals(ScreenId.LOGIN, navigator.current());
        assertEquals(List.of("Disconnected from server."), feedback);
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
