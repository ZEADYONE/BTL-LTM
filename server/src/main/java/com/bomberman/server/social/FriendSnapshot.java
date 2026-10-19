package com.bomberman.server.social;

import java.util.List;

record FriendSnapshot(
        List<FriendEntry> friends,
        List<FriendEntry> incomingRequests,
        List<FriendEntry> outgoingRequests
) {

    FriendSnapshot {
        friends = List.copyOf(friends);
        incomingRequests = List.copyOf(incomingRequests);
        outgoingRequests = List.copyOf(outgoingRequests);
    }
}
