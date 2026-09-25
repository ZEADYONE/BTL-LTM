package com.bomberman.common.dto;

import com.bomberman.common.enums.GameResult;

import java.util.List;

public record GameOverDto(
        Long winnerUserId,
        GameResult matchResult,
        List<GameOverPlayerDto> players
) {

    public GameOverDto {
        players = List.copyOf(players);
    }
}
