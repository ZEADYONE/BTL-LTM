package com.bomberman.common.dto;

import com.bomberman.common.enums.GameResult;

public record GameOverPlayerDto(long userId, GameResult result, int scoreEarnedUnits) {
}
