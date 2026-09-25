package com.bomberman.common.dto;

import com.bomberman.common.enums.GameResult;

import java.util.List;

public record MatchHistoryEntryDto(
        long matchId,
        String roomId,
        long startedAtEpochMillis,
        long endedAtEpochMillis,
        Long winnerUserId,
        GameResult result,
        List<MatchHistoryPlayerDto> players,
        GameResult viewerResult,
        int viewerScoreEarnedUnits
) {

    public MatchHistoryEntryDto {
        players = List.copyOf(players);
    }
}
