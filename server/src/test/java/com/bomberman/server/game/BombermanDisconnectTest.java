package com.bomberman.server.game;

import com.bomberman.common.enums.GameResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class BombermanDisconnectTest {

    @Test
    void disconnectedPlayerDiesAndGameContinuesWithMultipleSurvivors() {
        BombermanGame game = gameWithPlayers(1, 2, 3);

        GameOutcome outcome = game.disconnectPlayer(1);

        assertNull(outcome);
        assertFalse(game.getPlayer(1).isAlive());
        assertEquals(2, game.createSnapshot(1, java.time.Instant.now()).remainingPlayers());
    }

    @Test
    void lastConnectedSurvivorWins() {
        BombermanGame game = gameWithPlayers(1, 2);

        GameOutcome outcome = game.disconnectPlayer(1);

        assertEquals(GameResult.WIN, outcome.result());
        assertEquals(2L, outcome.winnerUserId());
    }

    @Test
    void allPlayersDisconnectingInSameCommandBatchProducesDraw() {
        BombermanGame game = gameWithPlayers(1, 2);

        game.disconnectPlayer(1);
        GameOutcome outcome = game.disconnectPlayer(2);

        assertEquals(GameResult.DRAW, outcome.result());
        assertNull(outcome.winnerUserId());
    }

    private BombermanGame gameWithPlayers(long... userIds) {
        return BombermanGame.createDefault(
                java.util.Arrays.stream(userIds)
                        .mapToObj(id -> new GameParticipant(id, "player-" + id))
                        .toList()
        );
    }
}
