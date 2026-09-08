package com.memorygame.repository;

import com.memorygame.model.PlayerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlayerProfileRepository extends JpaRepository<PlayerProfile, Long> {

    // Fetch profile by the associated user's id
    Optional<PlayerProfile> findByUserId(Long userId);
}
