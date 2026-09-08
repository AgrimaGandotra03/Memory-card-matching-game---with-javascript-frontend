package com.memorygame.repository;

import com.memorygame.model.ScoreRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScoreRecordRepository extends JpaRepository<ScoreRecord, Long> {

    /**
     * Returns all score records for a user, newest first.
     * Used by GET /api/scores/{userId}.
     */
    List<ScoreRecord> findByUserIdOrderByPlayedAtDesc(Long userId);
}
