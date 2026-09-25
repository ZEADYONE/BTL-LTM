package com.bomberman.common.dto;

import com.bomberman.common.enums.GameResult;

public record MatchHistoryPlayerDto(
        long userId,
        String username,
        GameResult result,
        int scoreEarnedUnits
) {
}
