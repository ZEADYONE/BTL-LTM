package com.bomberman.common.dto;

import java.util.List;

/** Complete social snapshot for one authenticated player. */
public record FriendListUpdate(
        List<FriendDto> friends,
        List<FriendRequestDto> incomingRequests,
        List<FriendRequestDto> outgoingRequests
) {

    public FriendListUpdate {
        friends = List.copyOf(friends);
        incomingRequests = List.copyOf(incomingRequests);
        outgoingRequests = List.copyOf(outgoingRequests);
    }
}
