package com.bomberman.server.social;

import com.bomberman.common.dto.CreateRoomRequest;
import com.bomberman.common.dto.FriendListUpdate;
import com.bomberman.common.dto.LoginRequest;
import com.bomberman.common.dto.LoginResponse;
import com.bomberman.common.dto.RegisterRequest;
import com.bomberman.common.dto.RegisterResponse;
import com.bomberman.common.dto.RemoveFriendRequest;
import com.bomberman.common.dto.RespondFriendRequest;
import com.bomberman.common.dto.SendFriendRequest;
import com.bomberman.common.dto.RoomStateUpdate;
import com.bomberman.common.enums.FriendPresenceStatus;
import com.bomberman.common.enums.MessageType;
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
class FriendMessageHandlerIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private TcpGameServer tcpGameServer;

    @Test
    void twoClientsCompleteFriendLifecycleAndReceivePushUpdates() throws Exception {
        String aliceName = uniqueUsername("alice");
        String bobName = uniqueUsername("bob");

        try (TestTcpClient alice = connect(); TestTcpClient bob = connect()) {
            LoginResponse aliceLogin = alice.registerAndLogin(aliceName);
            LoginResponse bobLogin = bob.registerAndLogin(bobName);

            FriendListUpdate aliceAfterSend = alice.friendCommand(
                    MessageType.FRIEND_REQUEST_SEND,
                    new SendFriendRequest(bobName)
            );
            assertEquals(bobLogin.userId(), aliceAfterSend.outgoingRequests().getFirst().userId());

            FriendListUpdate bobIncoming = bob.readFriendUpdate(
                    update -> !update.incomingRequests().isEmpty()
            );
            long relationshipId = bobIncoming.incomingRequests().getFirst().relationshipId();
            FriendListUpdate bobAfterAccept = bob.friendCommand(
                    MessageType.FRIEND_REQUEST_RESPOND,
                    new RespondFriendRequest(relationshipId, true)
            );
            assertEquals(aliceLogin.userId(), bobAfterAccept.friends().getFirst().userId());

            FriendListUpdate aliceAccepted = alice.readFriendUpdate(
                    update -> update.friends().stream()
                            .anyMatch(friend -> friend.userId() == bobLogin.userId())
            );
            assertEquals(1, aliceAccepted.friends().size());

            alice.exchange(
                    MessageType.CREATE_ROOM,
                    MessageType.ROOM_STATE,
                    new CreateRoomRequest("Presence room"),
                    RoomStateUpdate.class
            );
            FriendListUpdate bobSeesAliceInRoom = bob.readFriendUpdate(
                    update -> update.friends().stream().anyMatch(friend ->
                            friend.userId() == aliceLogin.userId()
                                    && friend.presence() == FriendPresenceStatus.IN_ROOM)
            );
            assertEquals(
                    FriendPresenceStatus.IN_ROOM,
                    bobSeesAliceInRoom.friends().getFirst().presence()
            );

            alice.exchange(
                    MessageType.LEAVE_ROOM,
                    MessageType.ROOM_STATE,
                    null,
                    RoomStateUpdate.class
            );
            FriendListUpdate bobSeesAliceFree = bob.readFriendUpdate(
                    update -> update.friends().stream().anyMatch(friend ->
                            friend.userId() == aliceLogin.userId()
                                    && friend.presence() == FriendPresenceStatus.FREE)
            );
            assertEquals(FriendPresenceStatus.FREE, bobSeesAliceFree.friends().getFirst().presence());

            FriendListUpdate aliceAfterRemove = alice.friendCommand(
                    MessageType.FRIEND_REMOVE,
                    new RemoveFriendRequest(bobLogin.userId())
            );
            assertTrue(aliceAfterRemove.friends().isEmpty());

            FriendListUpdate bobAfterRemove = bob.readFriendUpdate(
                    update -> update.friends().isEmpty()
                            && update.incomingRequests().isEmpty()
                            && update.outgoingRequests().isEmpty()
            );
            assertTrue(bobAfterRemove.friends().isEmpty());
        }
    }

    @Test
    void disconnectPushesOfflinePresenceToFriend() throws Exception {
        String aliceName = uniqueUsername("alice");
        String bobName = uniqueUsername("bob");

        try (TestTcpClient alice = connect(); TestTcpClient bob = connect()) {
            LoginResponse aliceLogin = alice.registerAndLogin(aliceName);
            bob.registerAndLogin(bobName);
            alice.friendCommand(MessageType.FRIEND_REQUEST_SEND, new SendFriendRequest(bobName));
            FriendListUpdate bobIncoming = bob.readFriendUpdate(
                    update -> !update.incomingRequests().isEmpty()
            );
            bob.friendCommand(
                    MessageType.FRIEND_REQUEST_RESPOND,
                    new RespondFriendRequest(
                            bobIncoming.incomingRequests().getFirst().relationshipId(),
                            true
                    )
            );
            alice.readFriendUpdate(update -> !update.friends().isEmpty());

            alice.close();

            FriendListUpdate bobSeesAliceOffline = bob.readFriendUpdate(
                    update -> update.friends().stream().anyMatch(friend ->
                            friend.userId() == aliceLogin.userId()
                                    && friend.presence() == FriendPresenceStatus.OFFLINE)
            );
            assertEquals(
                    FriendPresenceStatus.OFFLINE,
                    bobSeesAliceOffline.friends().getFirst().presence()
            );
        }
    }

    private TestTcpClient connect() throws IOException {
        return new TestTcpClient(tcpGameServer.getPort());
    }

    private String uniqueUsername(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private final class TestTcpClient implements AutoCloseable {

        private final Socket socket = new Socket();
        private final MessageEncoder encoder = new MessageEncoder(objectMapper);
        private final MessageDecoder decoder = new MessageDecoder(objectMapper);

        private TestTcpClient(int port) throws IOException {
            socket.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 2_000);
            socket.setSoTimeout(5_000);
        }

        private LoginResponse registerAndLogin(String username) throws IOException {
            exchange(
                    MessageType.REGISTER_REQUEST,
                    MessageType.REGISTER_RESPONSE,
                    new RegisterRequest(username, "password"),
                    RegisterResponse.class
            );
            return exchange(
                    MessageType.LOGIN_REQUEST,
                    MessageType.LOGIN_RESPONSE,
                    new LoginRequest(username, "password"),
                    LoginResponse.class
            );
        }

        private FriendListUpdate friendCommand(MessageType type, Object payload) throws IOException {
            return exchange(type, MessageType.FRIEND_LIST_UPDATE, payload, FriendListUpdate.class);
        }

        private FriendListUpdate readFriendUpdate(Predicate<FriendListUpdate> predicate) throws IOException {
            for (int attempt = 0; attempt < 40; attempt++) {
                NetworkMessage message = decoder.decode(socket.getInputStream());
                if (message.type() != MessageType.FRIEND_LIST_UPDATE || message.requestId() != null) {
                    continue;
                }
                FriendListUpdate update = objectMapper.treeToValue(
                        message.payload(),
                        FriendListUpdate.class
                );
                if (predicate.test(update)) {
                    return update;
                }
            }
            throw new IOException("Expected friend push was not received");
        }

        private <T> T exchange(
                MessageType requestType,
                MessageType responseType,
                Object payload,
                Class<T> responseClass
        ) throws IOException {
            String requestId = UUID.randomUUID().toString();
            encoder.encode(
                    new NetworkMessage(requestType, requestId, objectMapper.valueToTree(payload)),
                    socket.getOutputStream()
            );

            for (int attempt = 0; attempt < 40; attempt++) {
                NetworkMessage response = decoder.decode(socket.getInputStream());
                if (response.type() == responseType && requestId.equals(response.requestId())) {
                    return objectMapper.treeToValue(response.payload(), responseClass);
                }
            }
            throw new IOException("Expected correlated response was not received: " + responseType);
        }

        @Override
        public void close() throws IOException {
            socket.close();
        }
    }
}
