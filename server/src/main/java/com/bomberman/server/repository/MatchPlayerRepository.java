package com.bomberman.server.repository;

import com.bomberman.server.match.Match;
import com.bomberman.server.match.MatchPlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MatchPlayerRepository extends JpaRepository<MatchPlayer, Long> {

    @Query("""
            select distinct m
            from Match m
            join fetch m.players
            where m.id in (
                select mp.match.id from MatchPlayer mp where mp.userId = :userId
            )
            order by m.endedAt desc
            """)
    List<Match> findMatchesByUserId(@Param("userId") long userId);
}
