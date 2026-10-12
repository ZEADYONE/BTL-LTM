package com.bomberman.server.social;

import com.bomberman.common.dto.CreateRoomRequest;
import com.bomberman.common.dto.ErrorResponse;
import com.bomberman.common.dto.FriendListUpdate;
import com.bomberman.common.dto.LoginRequest;
import com.bomberman.common.dto.LoginResponse;
import com.bomberman.common.dto.RegisterRequest;
import com.bomberman.common.dto.RegisterResponse;
import com.bomberman.common.dto.RespondFriendRequest;
import com.bomberman.common.dto.RespondRoomInviteRequest;
import com.bomberman.common.dto.RoomInviteDto;
import com.bomberman.common.dto.RoomInviteStatusUpdate;
import com.bomberman.common.dto.RoomStateUpdate;
import com.bomberman.common.dto.SendFriendRequest;
import com.bomberman.common.dto.SendRoomInviteRequest;
import com.bomberman.common.enums.MessageType;
import com.bomberman.common.enums.RoomInviteResultCode;
import com.bomberman.common.enums.RoomInviteStatus;
import com.bomberman.common.message.NetworkMessage;
import com.bomberman.common.message.codec.MessageDecoder;
import com.bomberman.common.message.codec.MessageEncoder;
import com.bomberman.server.network.TcpGameServer;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "bomberman.tcp.port=0")
@Timeout(20)
class RoomInviteIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private TcpGameServer tcpGameServer;

    @Test
    void friendAcceptsInvitationAndIsJoinedServerSide() throws Exception {
        try (TestClient host = connect(); TestClient friend = connect()) {
            LoginResponse hostLogin = host.registerAndLogin(unique("host"));
            LoginResponse friendLogin = friend.registerAndLogin(unique("friend"));
            becomeFriends(host, friend, friendLogin.username());
            RoomStateUpdate created = host.exchange(
                    MessageType.CREATE_ROOM, new CreateRoomRequest("Invite party"),
                    MessageType.ROOM_STATE, RoomStateUpdate.class
            );

            RoomInviteStatusUpdate sent = host.exchange(
                    MessageType.ROOM_INVITE_SEND, new SendRoomInviteRequest(friendLogin.userId()),
                    MessageType.ROOM_INVITE_STATUS, RoomInviteStatusUpdate.class
            );
            assertEquals(RoomInviteStatus.SENT, sent.status());
            RoomInviteDto received = friend.read(
                    message -> message.type() == MessageType.ROOM_INVITE_RECEIVED,
                    RoomInviteDto.class
            );
            assertEquals(created.room().roomId(), received.roomId());
            assertEquals(hostLogin.userId(), received.inviterUserId());

            RoomStateUpdate joined = friend.exchange(
                    MessageType.ROOM_INVITE_RESPOND,
                    new RespondRoomInviteRequest(received.inviteId(), true),
                    MessageType.ROOM_STATE,
                    RoomStateUpdate.class
            );
            assertTrue(joined.member());
            assertEquals(2, joined.room().players().size());
            RoomInviteStatusUpdate accepted = host.read(message ->
                    message.type() == MessageType.ROOM_INVITE_STATUS
                            && message.requestId() == null
                            && message.payload().path("inviteId").asText().equals(received.inviteId()),
                    RoomInviteStatusUpdate.class
            );
            assertEquals(RoomInviteStatus.ACCEPTED, accepted.status());
        }
    }

    @Test
    void declinedInvitationCannotBeReused() throws Exception {
        try (TestClient host = connect(); TestClient friend = connect()) {
            LoginResponse friendLogin = friend.registerAndLogin(unique("decliner"));
            host.registerAndLogin(unique("inviter"));
            becomeFriends(host, friend, friendLogin.username());
            host.exchange(MessageType.CREATE_ROOM, new CreateRoomRequest("No thanks"),
                    MessageType.ROOM_STATE, RoomStateUpdate.class);
            host.exchange(MessageType.ROOM_INVITE_SEND, new SendRoomInviteRequest(friendLogin.userId()),
                    MessageType.ROOM_INVITE_STATUS, RoomInviteStatusUpdate.class);
            RoomInviteDto invite = friend.read(
                    message -> message.type() == MessageType.ROOM_INVITE_RECEIVED,
                    RoomInviteDto.class
            );

            RoomInviteStatusUpdate declined = friend.exchange(
                    MessageType.ROOM_INVITE_RESPOND,
                    new RespondRoomInviteRequest(invite.inviteId(), false),
                    MessageType.ROOM_INVITE_STATUS,
                    RoomInviteStatusUpdate.class
            );
            assertEquals(RoomInviteStatus.DECLINED, declined.status());

            ErrorResponse replay = friend.exchange(
                    MessageType.ROOM_INVITE_RESPOND,
                    new RespondRoomInviteRequest(invite.inviteId(), true),
                    MessageType.ERROR,
                    ErrorResponse.class
            );
            assertEquals(RoomInviteResultCode.INVITE_NOT_FOUND.name(), replay.code());
        }
    }

    private void becomeFriends(TestClient sender, TestClient recipient, String recipientName) throws IOException {
        sender.exchange(MessageType.FRIEND_REQUEST_SEND, new SendFriendRequest(recipientName),
                MessageType.FRIEND_LIST_UPDATE, FriendListUpdate.class);
        FriendListUpdate incoming = recipient.read(message ->
                message.type() == MessageType.FRIEND_LIST_UPDATE
                        && message.requestId() == null
                        && !message.payload().path("incomingRequests").isEmpty(),
                FriendListUpdate.class);
        recipient.exchange(MessageType.FRIEND_REQUEST_RESPOND,
                new RespondFriendRequest(incoming.incomingRequests().getFirst().relationshipId(), true),
                MessageType.FRIEND_LIST_UPDATE, FriendListUpdate.class);
    }

    private TestClient connect() throws IOException {
        return new TestClient(tcpGameServer.getPort());
    }

    private static String unique(String prefix) {
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
            exchange(MessageType.REGISTER_REQUEST, new RegisterRequest(username, "password"),
                    MessageType.REGISTER_RESPONSE, RegisterResponse.class);
            return exchange(MessageType.LOGIN_REQUEST, new LoginRequest(username, "password"),
                    MessageType.LOGIN_RESPONSE, LoginResponse.class);
        }

        private <T> T exchange(MessageType requestType, Object payload, MessageType responseType,
                               Class<T> responseClass) throws IOException {
            String requestId = UUID.randomUUID().toString();
            encoder.encode(new NetworkMessage(requestType, requestId,
                    payload == null ? null : objectMapper.valueToTree(payload)), socket.getOutputStream());
            return read(message -> responseType == message.type() && requestId.equals(message.requestId()),
                    responseClass);
        }

        private <T> T read(Predicate<NetworkMessage> predicate, Class<T> responseClass) throws IOException {
            for (int attempt = 0; attempt < 60; attempt++) {
                NetworkMessage message = decoder.decode(socket.getInputStream());
                if (predicate.test(message)) {
                    return objectMapper.treeToValue(message.payload(), responseClass);
                }
            }
            throw new IOException("Expected server message was not received");
        }

        @Override
        public void close() throws IOException {
            socket.close();
        }
    }
}
