package com.memorygame.repository;

import com.memorygame.model.GameSession;
import com.memorygame.model.GameStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GameSessionRepository extends JpaRepository<GameSession, Long> {

    /**
     * Returns the most recently updated ACTIVE or PAUSED session for a user.
     * Used by loadGame() to resume where the player left off.
     */
    Optional<GameSession> findTopByUserIdAndStatusInOrderByUpdatedAtDesc(
            Long userId, List<GameStatus> statuses);

    /**
     * All sessions for a user, newest first.
     * Useful for a history / debug view.
     */
    List<GameSession> findByUserIdOrderByCreatedAtDesc(Long userId);
}
