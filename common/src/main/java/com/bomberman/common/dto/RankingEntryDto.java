package com.bomberman.common.dto;

public record RankingEntryDto(
        int rank,
        long userId,
        String username,
        long totalScoreUnits,
        int totalWins,
        int totalLosses,
        int totalDraws
) {
}
