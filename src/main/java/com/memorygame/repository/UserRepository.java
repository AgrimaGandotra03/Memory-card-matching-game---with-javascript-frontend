package com.memorygame.repository;

import com.memorygame.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data will auto-generate the SQL for us.
 * We only need to declare the custom finder methods we want.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Used during login to look up by username
    Optional<User> findByUsername(String username);

    // Used during registration to check for duplicate usernames
    boolean existsByUsername(String username);
}
