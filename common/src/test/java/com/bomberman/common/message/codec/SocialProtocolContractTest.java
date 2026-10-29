package com.bomberman.common.message.codec;

import com.bomberman.common.dto.CancelFriendRequest;
import com.bomberman.common.dto.FriendDto;
import com.bomberman.common.dto.FriendListUpdate;
import com.bomberman.common.dto.FriendRequestDto;
import com.bomberman.common.dto.KickRoomMemberRequest;
import com.bomberman.common.dto.RemoveFriendRequest;
import com.bomberman.common.dto.RespondFriendRequest;
import com.bomberman.common.dto.RespondRoomInviteRequest;
import com.bomberman.common.dto.RoomInviteDto;
import com.bomberman.common.dto.RoomInviteStatusUpdate;
import com.bomberman.common.dto.RoomMemberRemovedNotice;
import com.bomberman.common.dto.SendFriendRequest;
import com.bomberman.common.dto.SendRoomInviteRequest;
import com.bomberman.common.enums.FriendPresenceStatus;
import com.bomberman.common.enums.MessageType;
import com.bomberman.common.enums.RoomInviteStatus;
import com.bomberman.common.enums.RoomRemovalReason;
import com.bomberman.common.message.NetworkMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SocialProtocolContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MessageEncoder encoder = new MessageEncoder(objectMapper);
    private final MessageDecoder decoder = new MessageDecoder(objectMapper);

    @Test
    void roundTripsFriendListSnapshot() throws IOException {
        FriendListUpdate expected = new FriendListUpdate(
                List.of(new FriendDto(2, "alice", FriendPresenceStatus.FREE)),
                List.of(new FriendRequestDto(
                        11,
                        3,
                        "bob",
                        FriendPresenceStatus.PLAYING,
                        1_760_000_000_000L
                )),
                List.of(new FriendRequestDto(
                        12,
                        4,
                        "carol",
                        FriendPresenceStatus.OFFLINE,
                        1_760_000_001_000L
                ))
        );

        assertPayloadRoundTrip(MessageType.FRIEND_LIST_UPDATE, expected, FriendListUpdate.class);
    }

    @Test
    void friendListSnapshotDefensivelyCopiesItsLists() {
        List<FriendDto> mutable = new ArrayList<>();
        FriendListUpdate snapshot = new FriendListUpdate(mutable, List.of(), List.of());

        mutable.add(new FriendDto(2, "alice", FriendPresenceStatus.FREE));

        assertEquals(List.of(), snapshot.friends());
        assertThrows(
                UnsupportedOperationException.class,
                () -> snapshot.friends().add(new FriendDto(3, "bob", FriendPresenceStatus.OFFLINE))
        );
    }

    @Test
    void roundTripsEveryFriendCommandPayload() throws IOException {
        assertPayloadRoundTrip(
                MessageType.FRIEND_REQUEST_SEND,
                new SendFriendRequest("alice"),
                SendFriendRequest.class
        );
        assertPayloadRoundTrip(
                MessageType.FRIEND_REQUEST_RESPOND,
                new RespondFriendRequest(21, true),
                RespondFriendRequest.class
        );
        assertPayloadRoundTrip(
                MessageType.FRIEND_REQUEST_CANCEL,
                new CancelFriendRequest(21),
                CancelFriendRequest.class
        );
        assertPayloadRoundTrip(
                MessageType.FRIEND_REMOVE,
                new RemoveFriendRequest(2),
                RemoveFriendRequest.class
        );
    }

    @Test
    void roundTripsRoomInvitePayloads() throws IOException {
        RoomInviteDto invite = new RoomInviteDto(
                "invite-1",
                "room-1",
                "Alice's room",
                2,
                "alice",
                1_760_000_060_000L
        );

        assertPayloadRoundTrip(
                MessageType.ROOM_INVITE_SEND,
                new SendRoomInviteRequest(3),
                SendRoomInviteRequest.class
        );
        assertPayloadRoundTrip(MessageType.ROOM_INVITE_RECEIVED, invite, RoomInviteDto.class);
        assertPayloadRoundTrip(
                MessageType.ROOM_INVITE_RESPOND,
                new RespondRoomInviteRequest("invite-1", false),
                RespondRoomInviteRequest.class
        );
        assertPayloadRoundTrip(
                MessageType.ROOM_INVITE_STATUS,
                new RoomInviteStatusUpdate("invite-1", RoomInviteStatus.DECLINED),
                RoomInviteStatusUpdate.class
        );
    }

    @Test
    void roundTripsKickAndRemovalPayloads() throws IOException {
        assertPayloadRoundTrip(
                MessageType.KICK_ROOM_MEMBER,
                new KickRoomMemberRequest(3),
                KickRoomMemberRequest.class
        );
        assertPayloadRoundTrip(
                MessageType.ROOM_MEMBER_REMOVED,
                new RoomMemberRemovedNotice(
                        "room-1",
                        "Alice's room",
                        RoomRemovalReason.HOST_KICK,
                        2,
                        "alice"
                ),
                RoomMemberRemovedNotice.class
        );
    }

    @Test
    void requestMessagesWithoutPayloadKeepNullPayload() throws IOException {
        NetworkMessage request = NetworkMessage.withoutPayload(
                MessageType.FRIEND_LIST_REQUEST,
                "request-1"
        );

        NetworkMessage decoded = decode(request);

        assertEquals(request, decoded);
        assertNull(decoded.payload());
    }

    private <T> void assertPayloadRoundTrip(
            MessageType messageType,
            T expectedPayload,
            Class<T> payloadType
    ) throws IOException {
        NetworkMessage original = new NetworkMessage(
                messageType,
                "request-1",
                objectMapper.valueToTree(expectedPayload)
        );

        NetworkMessage decoded = decode(original);
        T actualPayload = objectMapper.treeToValue(decoded.payload(), payloadType);

        assertEquals(messageType, decoded.type());
        assertEquals("request-1", decoded.requestId());
        assertEquals(expectedPayload, actualPayload);
    }

    private NetworkMessage decode(NetworkMessage message) throws IOException {
        return decoder.decode(new ByteArrayInputStream(encoder.encode(message)));
    }
}
