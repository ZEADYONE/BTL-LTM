package com.bomberman.server.repository;

import com.bomberman.server.match.Match;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MatchRepository extends JpaRepository<Match, Long> {

    Optional<Match> findByRoomId(String roomId);
}
