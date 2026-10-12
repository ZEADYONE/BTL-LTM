package com.bomberman.server.social;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Small in-memory sliding-window limiter for social commands. */
@Component
public class SocialRateLimiter {

    static final int MAX_FRIEND_REQUESTS_PER_WINDOW = 5;
    static final Duration FRIEND_REQUEST_WINDOW = Duration.ofMinutes(1);
    static final int MAX_ROOM_INVITES_PER_PAIR_WINDOW = 3;
    static final Duration ROOM_INVITE_WINDOW = Duration.ofMinutes(1);

    private record UserPair(long senderUserId, long recipientUserId) {
    }

    private final ConcurrentMap<Long, Deque<Instant>> friendRequests = new ConcurrentHashMap<>();
    private final ConcurrentMap<UserPair, Deque<Instant>> roomInvites = new ConcurrentHashMap<>();
    private final Clock clock;

    public SocialRateLimiter() {
        this(Clock.systemUTC());
    }

    SocialRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public boolean allowFriendRequest(long userId) {
        return allow(friendRequests.computeIfAbsent(userId, ignored -> new ArrayDeque<>()),
                MAX_FRIEND_REQUESTS_PER_WINDOW, FRIEND_REQUEST_WINDOW);
    }

    public boolean allowRoomInvite(long senderUserId, long recipientUserId) {
        UserPair pair = new UserPair(senderUserId, recipientUserId);
        return allow(roomInvites.computeIfAbsent(pair, ignored -> new ArrayDeque<>()),
                MAX_ROOM_INVITES_PER_PAIR_WINDOW, ROOM_INVITE_WINDOW);
    }

    private boolean allow(Deque<Instant> attempts, int limit, Duration window) {
        Instant now = clock.instant();
        Instant cutoff = now.minus(window);
        synchronized (attempts) {
            while (!attempts.isEmpty() && !attempts.peekFirst().isAfter(cutoff)) {
                attempts.removeFirst();
            }
            if (attempts.size() >= limit) {
                return false;
            }
            attempts.addLast(now);
            return true;
        }
    }
}
