package com.bomberman.server.social;

import com.bomberman.common.enums.FriendRelationshipStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendRelationshipRepository extends JpaRepository<FriendRelationship, Long> {

    @Query("""
            select relationship from FriendRelationship relationship
            join fetch relationship.userLow
            join fetch relationship.userHigh
            join fetch relationship.requestedBy
            where relationship.userLow.id = :lowId and relationship.userHigh.id = :highId
            """)
    Optional<FriendRelationship> findPair(
            @Param("lowId") long lowId,
            @Param("highId") long highId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select relationship from FriendRelationship relationship
            join fetch relationship.userLow
            join fetch relationship.userHigh
            join fetch relationship.requestedBy
            where relationship.userLow.id = :lowId and relationship.userHigh.id = :highId
            """)
    Optional<FriendRelationship> findPairForUpdate(
            @Param("lowId") long lowId,
            @Param("highId") long highId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select relationship from FriendRelationship relationship
            join fetch relationship.userLow
            join fetch relationship.userHigh
            join fetch relationship.requestedBy
            where relationship.id = :relationshipId
            """)
    Optional<FriendRelationship> findDetailedById(
            @Param("relationshipId") long relationshipId
    );

    @Query("""
            select relationship from FriendRelationship relationship
            join fetch relationship.userLow
            join fetch relationship.userHigh
            join fetch relationship.requestedBy
            where relationship.userLow.id = :userId or relationship.userHigh.id = :userId
            """)
    List<FriendRelationship> findAllForUser(@Param("userId") long userId);

    @Query("""
            select count(relationship) from FriendRelationship relationship
            where relationship.status = :status
              and (relationship.userLow.id = :userId or relationship.userHigh.id = :userId)
            """)
    long countForUserByStatus(
            @Param("userId") long userId,
            @Param("status") FriendRelationshipStatus status
    );

    long countByRequestedByIdAndStatus(long requestedById, FriendRelationshipStatus status);
}
