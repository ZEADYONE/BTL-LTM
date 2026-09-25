package com.bomberman.server.match;

import com.bomberman.common.enums.GameResult;
import com.bomberman.server.game.GameOutcome;
import com.bomberman.server.game.GameParticipant;
import com.bomberman.server.ranking.RankingService;
import com.bomberman.server.repository.MatchRepository;
import com.bomberman.server.repository.UserRepository;
import com.bomberman.server.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest(properties = "bomberman.tcp.port=0")
@Transactional
class MatchPersistenceIntegrationTest {

    @Autowired
    private MatchPersistenceService persistenceService;

    @Autowired
    private MatchHistoryService historyService;

    @Autowired
    private RankingService rankingService;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void winPersistsPlayersAndUpdatesRankingAndHistory() {
        User winner = saveUser("winner");
        User loser = saveUser("loser");
        Instant startedAt = Instant.parse("2026-01-01T10:00:00Z");
        Instant endedAt = startedAt.plusSeconds(90);

        Match saved = persistenceService.recordCompletedMatch(
                "room-win",
                startedAt,
                endedAt,
                List.of(
                        new GameParticipant(winner.getId(), winner.getUsername()),
                        new GameParticipant(loser.getId(), loser.getUsername())
                ),
                GameOutcome.win(winner.getId())
        );

        assertEquals(1, matchRepository.count());
        assertEquals(GameResult.WIN, saved.getResult());
        assertEquals(winner.getId(), saved.getWinnerUserId());
        assertEquals(2, saved.getPlayers().size());

        User updatedWinner = userRepository.findById(winner.getId()).orElseThrow();
        User updatedLoser = userRepository.findById(loser.getId()).orElseThrow();
        assertEquals(2, updatedWinner.getTotalScore());
        assertEquals(1, updatedWinner.getTotalWins());
        assertEquals(0, updatedLoser.getTotalScore());
        assertEquals(1, updatedLoser.getTotalLosses());

        var ranking = rankingService.getRanking().entries();
        assertEquals(winner.getId().longValue(), ranking.getFirst().userId());

        var history = historyService.getHistory(loser.getId()).matches();
        assertEquals(1, history.size());
        assertEquals(GameResult.LOSS, history.getFirst().viewerResult());
        assertEquals(0, history.getFirst().viewerScoreEarnedUnits());
        assertEquals(endedAt.toEpochMilli(), history.getFirst().endedAtEpochMillis());
    }

    @Test
    void drawAwardsHalfPointToEveryPlayer() {
        User first = saveUser("draw-a");
        User second = saveUser("draw-b");
        Instant startedAt = Instant.parse("2026-01-01T12:00:00Z");

        Match saved = persistenceService.recordCompletedMatch(
                "room-draw",
                startedAt,
                startedAt.plusSeconds(30),
                List.of(
                        new GameParticipant(first.getId(), first.getUsername()),
                        new GameParticipant(second.getId(), second.getUsername())
                ),
                GameOutcome.draw()
        );

        assertEquals(GameResult.DRAW, saved.getResult());
        assertNull(saved.getWinnerUserId());
        for (User player : userRepository.findAllById(List.of(first.getId(), second.getId()))) {
            assertEquals(1, player.getTotalScore());
            assertEquals(1, player.getTotalDraws());
        }
        saved.getPlayers().forEach(player -> {
            assertEquals(GameResult.DRAW, player.getResult());
            assertEquals(1, player.getScoreEarned());
        });
    }

    @Test
    void rankingUsesTotalScoreThenTotalWins() {
        User drawPlayer = saveUser("rank-draw");
        User winPlayer = saveUser("rank-win");
        drawPlayer.recordMatch(GameResult.DRAW, 1);
        drawPlayer.recordMatch(GameResult.DRAW, 1);
        winPlayer.recordMatch(GameResult.WIN, 2);
        userRepository.flush();

        var entries = rankingService.getRanking().entries();
        int winnerIndex = indexOf(entries, winPlayer.getId());
        int drawIndex = indexOf(entries, drawPlayer.getId());

        assertEquals(2, entries.get(winnerIndex).totalScoreUnits());
        assertEquals(2, entries.get(drawIndex).totalScoreUnits());
        org.junit.jupiter.api.Assertions.assertTrue(winnerIndex < drawIndex);
    }

    private User saveUser(String prefix) {
        return userRepository.saveAndFlush(new User(
                prefix + "_" + UUID.randomUUID().toString().substring(0, 8),
                "not-a-plaintext-password-hash"
        ));
    }

    private int indexOf(
            List<com.bomberman.common.dto.RankingEntryDto> entries,
            long userId
    ) {
        for (int index = 0; index < entries.size(); index++) {
            if (entries.get(index).userId() == userId) {
                return index;
            }
        }
        throw new AssertionError("Ranking entry not found for user " + userId);
    }
}
