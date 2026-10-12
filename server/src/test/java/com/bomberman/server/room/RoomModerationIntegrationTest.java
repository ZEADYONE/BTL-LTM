package com.bomberman.server.room;

import com.bomberman.common.dto.CreateRoomRequest;
import com.bomberman.common.dto.ErrorResponse;
import com.bomberman.common.dto.JoinRoomRequest;
import com.bomberman.common.dto.KickRoomMemberRequest;
import com.bomberman.common.dto.LoginRequest;
import com.bomberman.common.dto.LoginResponse;
import com.bomberman.common.dto.RegisterRequest;
import com.bomberman.common.dto.RegisterResponse;
import com.bomberman.common.dto.RoomMemberRemovedNotice;
import com.bomberman.common.dto.RoomStateUpdate;
import com.bomberman.common.enums.MessageType;
import com.bomberman.common.enums.PlayerStatus;
import com.bomberman.common.enums.RoomRemovalReason;
import com.bomberman.common.enums.RoomResultCode;
import com.bomberman.common.message.NetworkMessage;
import com.bomberman.common.message.codec.MessageDecoder;
import com.bomberman.common.message.codec.MessageEncoder;
import com.bomberman.server.network.TcpGameServer;
import com.bomberman.server.user.OnlineUserRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.UUID;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest(properties = "bomberman.tcp.port=0")
@Timeout(20)
class RoomModerationIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private TcpGameServer tcpGameServer;

    @Autowired
    private OnlineUserRegistry onlineUserRegistry;

    @Test
    void hostKickNotifiesTargetReturnsThemHomeAndBlocksDirectRejoin() throws Exception {
        try (TestClient host = connect(); TestClient guest = connect()) {
            LoginResponse hostLogin = host.registerAndLogin(uniqueUsername("host"));
            LoginResponse guestLogin = guest.registerAndLogin(uniqueUsername("guest"));
            RoomStateUpdate created = host.request(
                    MessageType.CREATE_ROOM,
                    new CreateRoomRequest("Kick room"),
                    MessageType.ROOM_STATE,
                    RoomStateUpdate.class
            );
            String roomId = created.room().roomId();
            guest.request(
                    MessageType.JOIN_ROOM,
                    new JoinRoomRequest(roomId),
                    MessageType.ROOM_STATE,
                    RoomStateUpdate.class
            );

            RoomStateUpdate hostAfterKick = host.request(
                    MessageType.KICK_ROOM_MEMBER,
                    new KickRoomMemberRequest(guestLogin.userId()),
                    MessageType.ROOM_STATE,
                    RoomStateUpdate.class
            );
            assertEquals(1, hostAfterKick.room().players().size());

            RoomMemberRemovedNotice notice = guest.await(
                    MessageType.ROOM_MEMBER_REMOVED,
                    ignored -> true,
                    RoomMemberRemovedNotice.class
            );
            assertEquals(roomId, notice.roomId());
            assertEquals(RoomRemovalReason.HOST_KICK, notice.reason());
            assertEquals(hostLogin.userId(), notice.actorUserId());
            assertEquals(hostLogin.username(), notice.actorUsername());

            RoomStateUpdate removedState = guest.await(
                    MessageType.ROOM_STATE,
                    update -> !update.member(),
                    RoomStateUpdate.class
            );
            assertFalse(removedState.member());
            assertNull(removedState.room());
            assertEquals(PlayerStatus.FREE, onlineUserRegistry.findStatus(guestLogin.userId()).orElseThrow());

            ErrorResponse error = guest.request(
                    MessageType.JOIN_ROOM,
                    new JoinRoomRequest(roomId),
                    MessageType.ERROR,
                    ErrorResponse.class
            );
            assertEquals(RoomResultCode.KICKED_FROM_ROOM.name(), error.code());

            RoomStateUpdate ownRoom = guest.request(
                    MessageType.CREATE_ROOM,
                    new CreateRoomRequest("Still logged in"),
                    MessageType.ROOM_STATE,
                    RoomStateUpdate.class
            );
            assertEquals(guestLogin.userId(), ownRoom.room().hostUserId());
        }
    }

    private TestClient connect() throws IOException {
        return new TestClient(tcpGameServer.getPort());
    }

    private static String uniqueUsername(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private final class TestClient implements AutoCloseable {

        private final Socket socket = new Socket();
        private final MessageEncoder encoder = new MessageEncoder(objectMapper);
        private final MessageDecoder decoder = new MessageDecoder(objectMapper);

        private TestClient(int port) throws IOException {
            socket.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 2_000);
            socket.setSoTimeout(5_000);
        }

        private LoginResponse registerAndLogin(String username) throws IOException {
            request(
                    MessageType.REGISTER_REQUEST,
                    new RegisterRequest(username, "password"),
                    MessageType.REGISTER_RESPONSE,
                    RegisterResponse.class
            );
            return request(
                    MessageType.LOGIN_REQUEST,
                    new LoginRequest(username, "password"),
                    MessageType.LOGIN_RESPONSE,
                    LoginResponse.class
            );
        }

        private <T> T request(
                MessageType requestType,
                Object payload,
                MessageType responseType,
                Class<T> responseClass
        ) throws IOException {
            String requestId = UUID.randomUUID().toString();
            encoder.encode(new NetworkMessage(
                    requestType,
                    requestId,
                    payload == null ? null : objectMapper.valueToTree(payload)
            ), socket.getOutputStream());
            return awaitMessage(
                    message -> message.type() == responseType && requestId.equals(message.requestId()),
                    responseClass
            );
        }

        private <T> T await(
                MessageType type,
                Predicate<T> predicate,
                Class<T> responseClass
        ) throws IOException {
            for (int attempt = 0; attempt < 80; attempt++) {
                NetworkMessage message = decoder.decode(socket.getInputStream());
                if (message.type() != type) {
                    continue;
                }
                T payload = objectMapper.treeToValue(message.payload(), responseClass);
                if (predicate.test(payload)) {
                    return payload;
                }
            }
            throw new IOException("Expected push was not received: " + type);
        }

        private <T> T awaitMessage(Predicate<NetworkMessage> predicate, Class<T> responseClass)
                throws IOException {
            for (int attempt = 0; attempt < 80; attempt++) {
                NetworkMessage message = decoder.decode(socket.getInputStream());
                if (predicate.test(message)) {
                    return objectMapper.treeToValue(message.payload(), responseClass);
                }
            }
            throw new IOException("Expected correlated response was not received");
        }

        @Override
        public void close() throws IOException {
            socket.close();
        }
    }
}
