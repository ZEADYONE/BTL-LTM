package com.bomberman.server.social;

import com.bomberman.common.enums.FriendResultCode;
import com.bomberman.server.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Set;

/** Validates and serializes friend commands before entering a DB transaction. */
@Service
public class FriendService {

    private static final int MAX_USERNAME_LENGTH = 50;
    private static final int LOCK_STRIPES = 256;

    private final FriendRelationshipStore store;
    private final UserRepository userRepository;
    private final SocialRateLimiter rateLimiter;
    private final Object[] pairLocks = new Object[LOCK_STRIPES];

    public FriendService(
            FriendRelationshipStore store,
            UserRepository userRepository,
            SocialRateLimiter rateLimiter
    ) {
        this.store = store;
        this.userRepository = userRepository;
        this.rateLimiter = rateLimiter;
        for (int index = 0; index < pairLocks.length; index++) {
            pairLocks[index] = new Object();
        }
    }

    public FriendCommandResult sendRequest(long requesterId, String rawUsername) {
        String username = rawUsername == null ? null : rawUsername.trim();
        if (username == null || username.isBlank() || username.length() > MAX_USERNAME_LENGTH) {
            return FriendCommandResult.failure(FriendResultCode.INVALID_REQUEST);
        }

        var target = userRepository.findByUsername(username).orElse(null);
        if (target == null) {
            return FriendCommandResult.failure(FriendResultCode.USER_NOT_FOUND);
        }
        if (target.getId() == requesterId) {
            return FriendCommandResult.failure(FriendResultCode.CANNOT_ADD_SELF);
        }
        if (!rateLimiter.allowFriendRequest(requesterId)) {
            return FriendCommandResult.failure(FriendResultCode.RATE_LIMITED);
        }

        synchronized (pairLock(requesterId, target.getId())) {
            try {
                return store.sendRequest(requesterId, target.getId());
            } catch (DataIntegrityViolationException concurrentInsert) {
                // Another server instance may have inserted the normalized pair first.
                // The failed transaction has already rolled back at this boundary, so retry
                // once in a fresh transaction and resolve the now-existing relationship.
                return store.sendRequest(requesterId, target.getId());
            }
        }
    }

    public FriendCommandResult respond(long actorId, long relationshipId, boolean accept) {
        return store.respond(actorId, relationshipId, accept);
    }

    public FriendCommandResult cancel(long actorId, long relationshipId) {
        return store.cancel(actorId, relationshipId);
    }

    public FriendCommandResult remove(long actorId, long friendUserId) {
        if (actorId == friendUserId) {
            return FriendCommandResult.failure(FriendResultCode.NOT_FRIENDS);
        }
        synchronized (pairLock(actorId, friendUserId)) {
            return store.remove(actorId, friendUserId);
        }
    }

    FriendSnapshot snapshot(long userId) {
        return store.snapshot(userId);
    }

    public Set<Long> acceptedFriendIds(long userId) {
        return store.acceptedFriendIds(userId);
    }

    public boolean areFriends(long firstUserId, long secondUserId) {
        return store.areFriends(firstUserId, secondUserId);
    }

    private Object pairLock(long firstUserId, long secondUserId) {
        long low = Math.min(firstUserId, secondUserId);
        long high = Math.max(firstUserId, secondUserId);
        int hash = Objects.hash(low, high);
        return pairLocks[Math.floorMod(hash, pairLocks.length)];
    }
}
