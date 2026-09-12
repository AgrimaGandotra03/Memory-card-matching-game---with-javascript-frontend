package com.memorygame.repository;

import com.memorygame.model.PlayerBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PlayerBadgeRepository extends JpaRepository<PlayerBadge, Long> {
    List<PlayerBadge> findByPlayerIdOrderByEarnedAtDesc(Long playerId);
    boolean existsByPlayerIdAndBadgeCode(Long playerId, String badgeCode);
}
