package com.memorygame.repository;

import com.memorygame.model.DailyChallengeCompletion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyChallengeCompletionRepository extends JpaRepository<DailyChallengeCompletion, Long> {
    Optional<DailyChallengeCompletion> findByPlayerIdAndChallengeDate(Long playerId, LocalDate challengeDate);
    List<DailyChallengeCompletion> findByPlayerIdOrderByChallengeDateDesc(Long playerId);
}
