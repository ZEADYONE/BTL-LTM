package com.bomberman.server.social;

import com.bomberman.common.enums.RoomInviteResultCode;
import com.bomberman.common.enums.RoomInviteStatus;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/** Owns active room invitations, expiry and lifecycle invalidation. */
@Component
public class RoomInviteManager {

    public static final Duration DEFAULT_TTL = Duration.ofSeconds(60);

    private record InviteKey(String roomId, long inviteeUserId) {
    }

    private final Object lock = new Object();
    private final Map<UUID, RoomInvite> activeById = new HashMap<>();
    private final Map<InviteKey, UUID> activeByRoomAndInvitee = new HashMap<>();
    private final Clock clock;
    private final Duration ttl;
    private final ScheduledExecutorService cleanupExecutor;
    private volatile BiConsumer<RoomInvite, RoomInviteStatus> terminalListener = (invite, status) -> { };

    public RoomInviteManager() {
        this(Clock.systemUTC(), DEFAULT_TTL, true);
    }

    RoomInviteManager(Clock clock, Duration ttl, boolean scheduleCleanup) {
        this.clock = clock;
        this.ttl = ttl;
        cleanupExecutor = Executors.newSingleThreadScheduledExecutor(
                Thread.ofPlatform().name("room-invite-cleanup").daemon().factory()
        );
        if (scheduleCleanup) {
            cleanupExecutor.scheduleAtFixedRate(this::expireNow, 1, 1, TimeUnit.SECONDS);
        }
    }

    public void setTerminalListener(BiConsumer<RoomInvite, RoomInviteStatus> listener) {
        terminalListener = listener == null ? (invite, status) -> { } : listener;
    }

    public RoomInviteCommandResult create(
            String roomId,
            String roomName,
            long inviterUserId,
            String inviterUsername,
            long inviteeUserId
    ) {
        List<RoomInvite> expired;
        RoomInviteCommandResult result;
        synchronized (lock) {
            expired = removeExpiredLocked(clock.instant());
            InviteKey key = new InviteKey(roomId, inviteeUserId);
            if (activeByRoomAndInvitee.containsKey(key)) {
                result = RoomInviteCommandResult.failure(RoomInviteResultCode.DUPLICATE_INVITE);
            } else {
                Instant now = clock.instant();
                RoomInvite invite = new RoomInvite(
                        UUID.randomUUID(),
                        roomId,
                        roomName,
                        inviterUserId,
                        inviterUsername,
                        inviteeUserId,
                        now,
                        now.plus(ttl)
                );
                activeById.put(invite.inviteId(), invite);
                activeByRoomAndInvitee.put(key, invite.inviteId());
                result = RoomInviteCommandResult.success(invite);
            }
        }
        notifyTerminal(expired, RoomInviteStatus.EXPIRED);
        return result;
    }

    /** Removes an invite exactly once after validating its recipient. */
    public RoomInviteCommandResult takeForResponse(String rawInviteId, long recipientUserId) {
        UUID inviteId;
        try {
            inviteId = UUID.fromString(rawInviteId);
        } catch (IllegalArgumentException | NullPointerException exception) {
            return RoomInviteCommandResult.failure(RoomInviteResultCode.INVITE_NOT_FOUND);
        }

        RoomInvite invite;
        boolean expired = false;
        synchronized (lock) {
            invite = activeById.get(inviteId);
            if (invite == null) {
                return RoomInviteCommandResult.failure(RoomInviteResultCode.INVITE_NOT_FOUND);
            }
            if (invite.inviteeUserId() != recipientUserId) {
                return RoomInviteCommandResult.failure(RoomInviteResultCode.INVITE_NOT_RECIPIENT);
            }
            if (!invite.expiresAt().isAfter(clock.instant())) {
                removeLocked(invite);
                expired = true;
            } else {
                removeLocked(invite);
            }
        }
        if (expired) {
            notifyTerminal(List.of(invite), RoomInviteStatus.EXPIRED);
            return RoomInviteCommandResult.failure(RoomInviteResultCode.INVITE_EXPIRED);
        }
        return RoomInviteCommandResult.success(invite);
    }

    public List<RoomInvite> cancelByRoom(String roomId) {
        return cancelMatching(invite -> invite.roomId().equals(roomId));
    }

    public List<RoomInvite> cancelByInviter(long inviterUserId) {
        return cancelMatching(invite -> invite.inviterUserId() == inviterUserId);
    }

    public List<RoomInvite> cancelForUser(long userId) {
        return cancelMatching(invite ->
                invite.inviterUserId() == userId || invite.inviteeUserId() == userId
        );
    }

    public List<RoomInvite> cancelForInvitee(long inviteeUserId) {
        return cancelMatching(invite -> invite.inviteeUserId() == inviteeUserId);
    }

    public int activeCount() {
        synchronized (lock) {
            return activeById.size();
        }
    }

    void expireNow() {
        List<RoomInvite> expired;
        synchronized (lock) {
            expired = removeExpiredLocked(clock.instant());
        }
        notifyTerminal(expired, RoomInviteStatus.EXPIRED);
    }

    private List<RoomInvite> cancelMatching(java.util.function.Predicate<RoomInvite> predicate) {
        List<RoomInvite> cancelled = new ArrayList<>();
        synchronized (lock) {
            for (RoomInvite invite : List.copyOf(activeById.values())) {
                if (predicate.test(invite)) {
                    removeLocked(invite);
                    cancelled.add(invite);
                }
            }
        }
        notifyTerminal(cancelled, RoomInviteStatus.CANCELLED);
        return List.copyOf(cancelled);
    }

    private List<RoomInvite> removeExpiredLocked(Instant now) {
        List<RoomInvite> expired = new ArrayList<>();
        for (RoomInvite invite : List.copyOf(activeById.values())) {
            if (!invite.expiresAt().isAfter(now)) {
                removeLocked(invite);
                expired.add(invite);
            }
        }
        return expired;
    }

    private void removeLocked(RoomInvite invite) {
        activeById.remove(invite.inviteId());
        activeByRoomAndInvitee.remove(new InviteKey(invite.roomId(), invite.inviteeUserId()));
    }

    private void notifyTerminal(List<RoomInvite> invites, RoomInviteStatus status) {
        invites.forEach(invite -> terminalListener.accept(invite, status));
    }

    @PreDestroy
    public void close() {
        cleanupExecutor.shutdownNow();
        synchronized (lock) {
            activeById.clear();
            activeByRoomAndInvitee.clear();
        }
    }
}
