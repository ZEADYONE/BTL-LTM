package com.bomberman.server.social;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SocialRateLimiterTest {

    @Test
    void limitsOneUserAndAllowsAgainAfterWindowExpires() {
        MutableClock clock = new MutableClock(Instant.parse("2026-10-12T00:00:00Z"));
        SocialRateLimiter limiter = new SocialRateLimiter(clock);

        for (int attempt = 0; attempt < SocialRateLimiter.MAX_FRIEND_REQUESTS_PER_WINDOW; attempt++) {
            assertTrue(limiter.allowFriendRequest(1));
        }
        assertFalse(limiter.allowFriendRequest(1));
        assertTrue(limiter.allowFriendRequest(2));

        clock.advance(SocialRateLimiter.FRIEND_REQUEST_WINDOW.plusMillis(1));

        assertTrue(limiter.allowFriendRequest(1));
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
