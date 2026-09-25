package com.bomberman.server.game;

import com.bomberman.common.enums.GameResult;

/** Winner information once an authoritative game has ended. */
public record GameOutcome(GameResult result, Long winnerUserId) {

    public static GameOutcome win(long winnerUserId) {
        return new GameOutcome(GameResult.WIN, winnerUserId);
    }

    public static GameOutcome draw() {
        return new GameOutcome(GameResult.DRAW, null);
    }
}
