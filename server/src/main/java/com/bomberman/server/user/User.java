package com.bomberman.server.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

import com.bomberman.common.enums.GameResult;

/**
 * Persisted player account. This entity never crosses the network boundary.
 */
@Entity
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(name = "uk_users_username", columnNames = "username")
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false, length = 60)
    private String passwordHash;

    @Column(nullable = false)
    private long totalScore;

    @Column(nullable = false)
    private int totalWins;

    @Column(nullable = false)
    private int totalLosses;

    @Column(nullable = false)
    private int totalDraws;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected User() {
    }

    public User(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
    }

    @PrePersist
    void initializeCreatedAt() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public long getTotalScore() {
        return totalScore;
    }

    public int getTotalWins() {
        return totalWins;
    }

    public int getTotalLosses() {
        return totalLosses;
    }

    public int getTotalDraws() {
        return totalDraws;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void recordMatch(GameResult result, int scoreEarnedUnits) {
        if (scoreEarnedUnits < 0) {
            throw new IllegalArgumentException("scoreEarnedUnits must not be negative");
        }
        totalScore += scoreEarnedUnits;
        switch (result) {
            case WIN -> totalWins++;
            case LOSS -> totalLosses++;
            case DRAW -> totalDraws++;
        }
    }
}
