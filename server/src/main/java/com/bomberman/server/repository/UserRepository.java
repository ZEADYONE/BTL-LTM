package com.bomberman.server.repository;

import com.bomberman.server.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    List<User> findAllByOrderByTotalScoreDescTotalWinsDescUsernameAsc();
}
