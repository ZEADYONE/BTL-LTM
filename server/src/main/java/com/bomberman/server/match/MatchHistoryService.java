package com.bomberman.server.match;

import com.bomberman.common.dto.HistoryResponse;
import com.bomberman.common.dto.MatchHistoryEntryDto;
import com.bomberman.common.dto.MatchHistoryPlayerDto;
import com.bomberman.server.repository.MatchPlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatchHistoryService {

    private final MatchPlayerRepository matchPlayerRepository;

    public MatchHistoryService(MatchPlayerRepository matchPlayerRepository) {
        this.matchPlayerRepository = matchPlayerRepository;
    }

    @Transactional(readOnly = true)
    public HistoryResponse getHistory(long userId) {
        return new HistoryResponse(
                matchPlayerRepository.findMatchesByUserId(userId).stream()
                        .map(match -> toDto(match, userId))
                        .toList()
        );
    }

    private MatchHistoryEntryDto toDto(Match match, long viewerUserId) {
        MatchPlayer viewer = match.getPlayers().stream()
                .filter(player -> player.getUserId() == viewerUserId)
                .findFirst()
                .orElseThrow();
        return new MatchHistoryEntryDto(
                match.getId(),
                match.getRoomId(),
                match.getStartedAt().toEpochMilli(),
                match.getEndedAt().toEpochMilli(),
                match.getWinnerUserId(),
                match.getResult(),
                match.getPlayers().stream()
                        .map(player -> new MatchHistoryPlayerDto(
                                player.getUserId(),
                                player.getUsername(),
                                player.getResult(),
                                player.getScoreEarned()
                        ))
                        .toList(),
                viewer.getResult(),
                viewer.getScoreEarned()
        );
    }
}
