package com.bomberman.server.social;

import com.bomberman.common.enums.FriendRelationshipStatus;
import com.bomberman.server.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.time.Instant;

/** Persisted relationship for one normalized, unordered pair of users. */
@Entity
@Table(
        name = "friend_relationships",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_friend_relationship_pair",
                columnNames = {"user_low_id", "user_high_id"}
        ),
        indexes = {
                @Index(
                        name = "ix_friend_relationship_low_status",
                        columnList = "user_low_id,status"
                ),
                @Index(
                        name = "ix_friend_relationship_high_status",
                        columnList = "user_high_id,status"
                )
        }
)
public class FriendRelationship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_low_id", nullable = false)
    private User userLow;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_high_id", nullable = false)
    private User userHigh;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by_user_id", nullable = false)
    private User requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private FriendRelationshipStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected FriendRelationship() {
    }

    FriendRelationship(User userLow, User userHigh, User requestedBy) {
        if (userLow.getId() >= userHigh.getId()) {
            throw new IllegalArgumentException("Friend pair must be normalized by user id");
        }
        if (!requestedBy.getId().equals(userLow.getId())
                && !requestedBy.getId().equals(userHigh.getId())) {
            throw new IllegalArgumentException("Requester must belong to the friend pair");
        }
        this.userLow = userLow;
        this.userHigh = userHigh;
        this.requestedBy = requestedBy;
        this.status = FriendRelationshipStatus.PENDING;
    }

    @PrePersist
    void initializeTimestamps() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void updateTimestamp() {
        updatedAt = Instant.now();
    }

    void accept() {
        status = FriendRelationshipStatus.ACCEPTED;
    }

    public Long getId() {
        return id;
    }

    public User getUserLow() {
        return userLow;
    }

    public User getUserHigh() {
        return userHigh;
    }

    public User getRequestedBy() {
        return requestedBy;
    }

    public FriendRelationshipStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }

    boolean contains(long userId) {
        return userLow.getId() == userId || userHigh.getId() == userId;
    }

    User otherUser(long userId) {
        if (userLow.getId() == userId) {
            return userHigh;
        }
        if (userHigh.getId() == userId) {
            return userLow;
        }
        throw new IllegalArgumentException("User does not belong to relationship");
    }
}
