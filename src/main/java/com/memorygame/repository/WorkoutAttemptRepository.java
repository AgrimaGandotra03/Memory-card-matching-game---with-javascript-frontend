package com.memorygame.repository;

import com.memorygame.model.WorkoutAttempt;
import com.memorygame.model.WorkoutSkill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface WorkoutAttemptRepository extends JpaRepository<WorkoutAttempt, Long> {

    List<WorkoutAttempt> findByPlayerIdOrderByCreatedAtDesc(Long playerId);

    List<WorkoutAttempt> findByPlayerIdAndSkillOrderByCreatedAtDesc(Long playerId, WorkoutSkill skill);

    List<WorkoutAttempt> findByPlayerIdAndAttemptDateOrderByCreatedAtDesc(Long playerId, LocalDate attemptDate);

    List<WorkoutAttempt> findByPlayerIdAndAttemptDateAndSkillOrderByCreatedAtDesc(
            Long playerId, LocalDate attemptDate, WorkoutSkill skill);
}
