package com.bomberman.server.match;

import com.bomberman.common.enums.GameResult;
import com.bomberman.server.game.GameOutcome;
import com.bomberman.server.game.GameParticipant;
import com.bomberman.server.repository.MatchRepository;
import com.bomberman.server.repository.UserRepository;
import com.bomberman.server.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Persists a completed match and updates all denormalized ranking counters atomically. */
@Service
public class MatchPersistenceService {

    private final MatchRepository matchRepository;
    private final UserRepository userRepository;

    public MatchPersistenceService(
            MatchRepository matchRepository,
            UserRepository userRepository
    ) {
        this.matchRepository = matchRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Match recordCompletedMatch(
            String roomId,
            Instant startedAt,
            Instant endedAt,
            List<GameParticipant> participants,
            GameOutcome outcome
    ) {
        List<Long> userIds = participants.stream().map(GameParticipant::userId).toList();
        Map<Long, User> usersById = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        if (usersById.size() != userIds.size()) {
            throw new IllegalStateException("Cannot persist a match with an unknown user");
        }

        Match match = new Match(
                roomId,
                startedAt,
                endedAt,
                outcome.winnerUserId(),
                outcome.result()
        );
        for (GameParticipant participant : participants) {
            GameResult playerResult = MatchScoring.playerResult(
                    participant.userId(),
                    outcome.result(),
                    outcome.winnerUserId()
            );
            int scoreUnits = MatchScoring.scoreUnits(playerResult);
            match.addPlayer(new MatchPlayer(
                    participant.userId(),
                    participant.username(),
                    playerResult,
                    scoreUnits
            ));
            usersById.get(participant.userId()).recordMatch(playerResult, scoreUnits);
        }
        return matchRepository.saveAndFlush(match);
    }

}
