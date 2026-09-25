package com.bomberman.server.match;

import com.bomberman.common.enums.GameResult;

/** Scores are stored in half-point units: 2 = 1.0 point, 1 = 0.5 point. */
public final class MatchScoring {

    public static final int WIN_SCORE_UNITS = 2;
    public static final int DRAW_SCORE_UNITS = 1;
    public static final int LOSS_SCORE_UNITS = 0;

    private MatchScoring() {
    }

    public static int scoreUnits(GameResult result) {
        return switch (result) {
            case WIN -> WIN_SCORE_UNITS;
            case DRAW -> DRAW_SCORE_UNITS;
            case LOSS -> LOSS_SCORE_UNITS;
        };
    }

    public static GameResult playerResult(long userId, GameResult matchResult, Long winnerUserId) {
        if (matchResult == GameResult.DRAW) {
            return GameResult.DRAW;
        }
        return winnerUserId != null && winnerUserId == userId
                ? GameResult.WIN
                : GameResult.LOSS;
    }
}
