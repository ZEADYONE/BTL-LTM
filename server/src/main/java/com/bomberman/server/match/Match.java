package com.bomberman.server.match;

import com.bomberman.common.enums.GameResult;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Persisted summary of one completed authoritative game. */
@Entity
@Table(name = "matches")
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 36)
    private String roomId;

    @Column(nullable = false)
    private Instant startedAt;

    @Column(nullable = false)
    private Instant endedAt;

    private Long winnerUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private GameResult result;

    @OneToMany(
            mappedBy = "match",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<MatchPlayer> players = new ArrayList<>();

    protected Match() {
    }

    public Match(
            String roomId,
            Instant startedAt,
            Instant endedAt,
            Long winnerUserId,
            GameResult result
    ) {
        this.roomId = roomId;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.winnerUserId = winnerUserId;
        this.result = result;
    }

    public void addPlayer(MatchPlayer player) {
        players.add(player);
        player.attachTo(this);
    }

    public Long getId() {
        return id;
    }

    public String getRoomId() {
        return roomId;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public Long getWinnerUserId() {
        return winnerUserId;
    }

    public GameResult getResult() {
        return result;
    }

    public List<MatchPlayer> getPlayers() {
        return Collections.unmodifiableList(players);
    }
}
