package com.bomberman.common.dto;

import java.util.List;

public record HistoryResponse(List<MatchHistoryEntryDto> matches) {

    public HistoryResponse {
        matches = List.copyOf(matches);
    }
}
