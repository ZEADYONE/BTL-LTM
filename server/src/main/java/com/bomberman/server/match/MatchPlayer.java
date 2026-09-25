package com.bomberman.server.match;

import com.bomberman.common.enums.GameResult;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "match_players",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_match_players_match_user",
                columnNames = {"match_id", "user_id"}
        )
)
public class MatchPlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @Column(name = "user_id", nullable = false)
    private long userId;

    @Column(nullable = false, length = 50)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private GameResult result;

    @Column(nullable = false)
    private int scoreEarned;

    protected MatchPlayer() {
    }

    public MatchPlayer(long userId, String username, GameResult result, int scoreEarned) {
        this.userId = userId;
        this.username = username;
        this.result = result;
        this.scoreEarned = scoreEarned;
    }

    void attachTo(Match match) {
        this.match = match;
    }

    public Long getId() {
        return id;
    }

    public Match getMatch() {
        return match;
    }

    public long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public GameResult getResult() {
        return result;
    }

    public int getScoreEarned() {
        return scoreEarned;
    }
}
