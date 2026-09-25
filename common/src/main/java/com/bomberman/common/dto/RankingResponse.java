package com.bomberman.common.dto;

import java.util.List;

public record RankingResponse(List<RankingEntryDto> entries) {

    public RankingResponse {
        entries = List.copyOf(entries);
    }
}
