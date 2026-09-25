package com.bomberman.server.ranking;

import com.bomberman.common.dto.RankingEntryDto;
import com.bomberman.common.dto.RankingResponse;
import com.bomberman.server.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class RankingService {

    private final UserRepository userRepository;

    public RankingService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public RankingResponse getRanking() {
        List<RankingEntryDto> entries = new ArrayList<>();
        var users = userRepository.findAllByOrderByTotalScoreDescTotalWinsDescUsernameAsc();
        for (int index = 0; index < users.size(); index++) {
            var user = users.get(index);
            entries.add(new RankingEntryDto(
                    index + 1,
                    user.getId(),
                    user.getUsername(),
                    user.getTotalScore(),
                    user.getTotalWins(),
                    user.getTotalLosses(),
                    user.getTotalDraws()
            ));
        }
        return new RankingResponse(entries);
    }
}
