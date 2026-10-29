package com.bomberman.server.social;

import com.bomberman.common.enums.FriendRelationshipStatus;
import com.bomberman.common.enums.FriendResultCode;
import com.bomberman.server.repository.UserRepository;
import com.bomberman.server.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Transaction boundary for persisted friend relationships. */
@Service
class FriendRelationshipStore {

    static final int MAX_FRIENDS = 100;
    static final int MAX_PENDING_OUTGOING = 20;

    private final FriendRelationshipRepository relationshipRepository;
    private final UserRepository userRepository;

    FriendRelationshipStore(
            FriendRelationshipRepository relationshipRepository,
            UserRepository userRepository
    ) {
        this.relationshipRepository = relationshipRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public FriendCommandResult sendRequest(long requesterId, long targetId) {
        long lowId = Math.min(requesterId, targetId);
        long highId = Math.max(requesterId, targetId);
        FriendRelationship existing = relationshipRepository.findPairForUpdate(lowId, highId).orElse(null);
        if (existing != null) {
            if (existing.getStatus() == FriendRelationshipStatus.ACCEPTED) {
                return FriendCommandResult.failure(FriendResultCode.ALREADY_FRIENDS);
            }
            if (existing.getRequestedBy().getId() == requesterId) {
                return FriendCommandResult.failure(FriendResultCode.REQUEST_ALREADY_PENDING);
            }
            if (friendLimitReached(requesterId) || friendLimitReached(targetId)) {
                return FriendCommandResult.failure(FriendResultCode.FRIEND_LIMIT_REACHED);
            }
            existing.accept();
            relationshipRepository.flush();
            return FriendCommandResult.success(targetId);
        }

        if (relationshipRepository.countByRequestedByIdAndStatus(
                requesterId,
                FriendRelationshipStatus.PENDING
        ) >= MAX_PENDING_OUTGOING) {
            return FriendCommandResult.failure(FriendResultCode.PENDING_LIMIT_REACHED);
        }
        if (friendLimitReached(requesterId) || friendLimitReached(targetId)) {
            return FriendCommandResult.failure(FriendResultCode.FRIEND_LIMIT_REACHED);
        }

        User requester = userRepository.findById(requesterId).orElse(null);
        User target = userRepository.findById(targetId).orElse(null);
        if (requester == null || target == null) {
            return FriendCommandResult.failure(FriendResultCode.USER_NOT_FOUND);
        }
        User low = requester.getId() < target.getId() ? requester : target;
        User high = low == requester ? target : requester;
        relationshipRepository.saveAndFlush(new FriendRelationship(low, high, requester));
        return FriendCommandResult.success(targetId);
    }

    @Transactional
    public FriendCommandResult respond(long actorId, long relationshipId, boolean accept) {
        FriendRelationship relationship = relationshipRepository.findDetailedById(relationshipId)
                .orElse(null);
        if (relationship == null || relationship.getStatus() != FriendRelationshipStatus.PENDING) {
            return FriendCommandResult.failure(FriendResultCode.REQUEST_NOT_FOUND);
        }
        if (!relationship.contains(actorId)
                || relationship.getRequestedBy().getId() == actorId) {
            return FriendCommandResult.failure(FriendResultCode.NOT_REQUEST_RECIPIENT);
        }

        long otherUserId = relationship.otherUser(actorId).getId();
        if (accept) {
            if (friendLimitReached(actorId) || friendLimitReached(otherUserId)) {
                return FriendCommandResult.failure(FriendResultCode.FRIEND_LIMIT_REACHED);
            }
            relationship.accept();
            relationshipRepository.flush();
        } else {
            relationshipRepository.delete(relationship);
            relationshipRepository.flush();
        }
        return FriendCommandResult.success(otherUserId);
    }

    @Transactional
    public FriendCommandResult cancel(long actorId, long relationshipId) {
        FriendRelationship relationship = relationshipRepository.findDetailedById(relationshipId)
                .orElse(null);
        if (relationship == null || relationship.getStatus() != FriendRelationshipStatus.PENDING) {
            return FriendCommandResult.failure(FriendResultCode.REQUEST_NOT_FOUND);
        }
        if (!relationship.contains(actorId)
                || relationship.getRequestedBy().getId() != actorId) {
            return FriendCommandResult.failure(FriendResultCode.NOT_REQUEST_SENDER);
        }

        long otherUserId = relationship.otherUser(actorId).getId();
        relationshipRepository.delete(relationship);
        relationshipRepository.flush();
        return FriendCommandResult.success(otherUserId);
    }

    @Transactional
    public FriendCommandResult remove(long actorId, long friendUserId) {
        long lowId = Math.min(actorId, friendUserId);
        long highId = Math.max(actorId, friendUserId);
        FriendRelationship relationship = relationshipRepository.findPairForUpdate(lowId, highId).orElse(null);
        if (relationship == null || relationship.getStatus() != FriendRelationshipStatus.ACCEPTED) {
            return FriendCommandResult.failure(FriendResultCode.NOT_FRIENDS);
        }

        relationshipRepository.delete(relationship);
        relationshipRepository.flush();
        return FriendCommandResult.success(friendUserId);
    }

    @Transactional(readOnly = true)
    public FriendSnapshot snapshot(long userId) {
        List<FriendEntry> friends = new ArrayList<>();
        List<FriendEntry> incoming = new ArrayList<>();
        List<FriendEntry> outgoing = new ArrayList<>();

        for (FriendRelationship relationship : relationshipRepository.findAllForUser(userId)) {
            User other = relationship.otherUser(userId);
            FriendEntry entry = new FriendEntry(
                    relationship.getId(),
                    other.getId(),
                    other.getUsername(),
                    relationship.getCreatedAt().toEpochMilli()
            );
            if (relationship.getStatus() == FriendRelationshipStatus.ACCEPTED) {
                friends.add(entry);
            } else if (relationship.getRequestedBy().getId() == userId) {
                outgoing.add(entry);
            } else {
                incoming.add(entry);
            }
        }

        Comparator<FriendEntry> newestFirst = Comparator
                .comparingLong(FriendEntry::requestedAtEpochMillis)
                .reversed();
        incoming.sort(newestFirst);
        outgoing.sort(newestFirst);
        return new FriendSnapshot(friends, incoming, outgoing);
    }

    @Transactional(readOnly = true)
    public Set<Long> acceptedFriendIds(long userId) {
        Set<Long> friendIds = new LinkedHashSet<>();
        relationshipRepository.findAllForUser(userId).stream()
                .filter(relationship -> relationship.getStatus() == FriendRelationshipStatus.ACCEPTED)
                .map(relationship -> relationship.otherUser(userId).getId())
                .forEach(friendIds::add);
        return Set.copyOf(friendIds);
    }

    @Transactional(readOnly = true)
    public boolean areFriends(long firstUserId, long secondUserId) {
        if (firstUserId == secondUserId) {
            return false;
        }
        return relationshipRepository.findPair(
                        Math.min(firstUserId, secondUserId),
                        Math.max(firstUserId, secondUserId)
                )
                .filter(relationship -> relationship.getStatus() == FriendRelationshipStatus.ACCEPTED)
                .isPresent();
    }

    private boolean friendLimitReached(long userId) {
        return relationshipRepository.countForUserByStatus(
                userId,
                FriendRelationshipStatus.ACCEPTED
        ) >= MAX_FRIENDS;
    }
}
