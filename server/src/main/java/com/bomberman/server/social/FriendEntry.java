package com.bomberman.server.social;

record FriendEntry(
        long relationshipId,
        long userId,
        String username,
        long requestedAtEpochMillis
) {
}
