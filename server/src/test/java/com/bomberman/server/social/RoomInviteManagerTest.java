package com.bomberman.server.social;

import com.bomberman.common.enums.RoomInviteResultCode;
import com.bomberman.common.enums.RoomInviteStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoomInviteManagerTest {

    @Test
    void inviteIsSingleUseAndScopedToItsRecipient() {
        try (Fixture fixture = new Fixture()) {
            RoomInvite invite = fixture.manager.create("room-1", "Party", 1, "host", 2).invite();

            assertEquals(RoomInviteResultCode.INVITE_NOT_RECIPIENT,
                    fixture.manager.takeForResponse(invite.inviteId().toString(), 3).result());
            assertTrue(fixture.manager.takeForResponse(invite.inviteId().toString(), 2).isSuccess());
            assertEquals(RoomInviteResultCode.INVITE_NOT_FOUND,
                    fixture.manager.takeForResponse(invite.inviteId().toString(), 2).result());
        }
    }

    @Test
    void duplicateIsRejectedUntilOriginalIsConsumed() {
        try (Fixture fixture = new Fixture()) {
            RoomInvite first = fixture.manager.create("room-1", "Party", 1, "host", 2).invite();

            assertEquals(RoomInviteResultCode.DUPLICATE_INVITE,
                    fixture.manager.create("room-1", "Party", 3, "member", 2).result());
            fixture.manager.takeForResponse(first.inviteId().toString(), 2);
            assertTrue(fixture.manager.create("room-1", "Party", 3, "member", 2).isSuccess());
        }
    }

    @Test
    void lazyExpiryRemovesInviteAndPublishesTerminalStatus() {
        try (Fixture fixture = new Fixture()) {
            RoomInvite invite = fixture.manager.create("room-1", "Party", 1, "host", 2).invite();
            fixture.clock.advance(Duration.ofSeconds(61));

            assertEquals(RoomInviteResultCode.INVITE_EXPIRED,
                    fixture.manager.takeForResponse(invite.inviteId().toString(), 2).result());
            assertEquals(List.of(RoomInviteStatus.EXPIRED), fixture.statuses);
            assertEquals(0, fixture.manager.activeCount());
        }
    }

    @Test
    void lifecycleCancellationPublishesOnce() {
        try (Fixture fixture = new Fixture()) {
            fixture.manager.create("room-1", "Party", 1, "host", 2);
            fixture.manager.create("room-1", "Party", 1, "host", 3);

            assertEquals(2, fixture.manager.cancelByRoom("room-1").size());
            assertEquals(List.of(RoomInviteStatus.CANCELLED, RoomInviteStatus.CANCELLED), fixture.statuses);
            assertTrue(fixture.manager.cancelByRoom("room-1").isEmpty());
        }
    }

    private static final class Fixture implements AutoCloseable {
        private final MutableClock clock = new MutableClock(Instant.parse("2026-10-12T00:00:00Z"));
        private final RoomInviteManager manager = new RoomInviteManager(clock, Duration.ofSeconds(60), false);
        private final List<RoomInviteStatus> statuses = new ArrayList<>();

        private Fixture() {
            manager.setTerminalListener((invite, status) -> statuses.add(status));
        }

        @Override
        public void close() {
            manager.close();
        }
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override public ZoneId getZone() { return ZoneId.of("UTC"); }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return instant; }
    }
}
