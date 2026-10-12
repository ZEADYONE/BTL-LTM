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

    private final ConcurrentMap<Long, Deque<Instant>> friendRequests = new ConcurrentHashMap<>();
    private final Clock clock;

    public SocialRateLimiter() {
        this(Clock.systemUTC());
    }

    SocialRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public boolean allowFriendRequest(long userId) {
        Instant now = clock.instant();
        Instant cutoff = now.minus(FRIEND_REQUEST_WINDOW);
        Deque<Instant> attempts = friendRequests.computeIfAbsent(userId, ignored -> new ArrayDeque<>());
        synchronized (attempts) {
            while (!attempts.isEmpty() && !attempts.peekFirst().isAfter(cutoff)) {
                attempts.removeFirst();
            }
            if (attempts.size() >= MAX_FRIEND_REQUESTS_PER_WINDOW) {
                return false;
            }
            attempts.addLast(now);
            return true;
        }
    }
}
