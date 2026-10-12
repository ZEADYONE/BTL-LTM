package com.bomberman.clientfx.state;

import com.bomberman.common.dto.FriendDto;
import com.bomberman.common.dto.FriendListUpdate;
import com.bomberman.common.dto.FriendRequestDto;
import com.bomberman.common.enums.FriendPresenceStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientStateSocialTest {

    @Test
    void friendSnapshotNotifiesAndExposesImmutableLists() {
        ClientState state = new ClientState();
        AtomicInteger changes = new AtomicInteger();
        state.addListener(changes::incrementAndGet);
        FriendListUpdate update = new FriendListUpdate(
                List.of(new FriendDto(2, "alice", FriendPresenceStatus.FREE)),
                List.of(new FriendRequestDto(
                        10,
                        3,
                        "bob",
                        FriendPresenceStatus.OFFLINE,
                        1_760_000_000_000L
                )),
                List.of()
        );

        state.setFriendList(update);

        assertEquals(1, changes.get());
        assertEquals(update.friends(), state.getFriends());
        assertEquals(update.incomingRequests(), state.getIncomingFriendRequests());
        assertThrows(
                UnsupportedOperationException.class,
                () -> state.getFriends().add(new FriendDto(4, "carol", FriendPresenceStatus.FREE))
        );
    }

    @Test
    void logoutClearsAllSocialState() {
        ClientState state = new ClientState();
        state.login(1, "me");
        state.setFriendList(new FriendListUpdate(
                List.of(new FriendDto(2, "alice", FriendPresenceStatus.FREE)),
                List.of(),
                List.of(new FriendRequestDto(
                        10,
                        3,
                        "bob",
                        FriendPresenceStatus.OFFLINE,
                        1_760_000_000_000L
                ))
        ));

        state.logout();

        assertTrue(state.getFriends().isEmpty());
        assertTrue(state.getIncomingFriendRequests().isEmpty());
        assertTrue(state.getOutgoingFriendRequests().isEmpty());
    }
}
