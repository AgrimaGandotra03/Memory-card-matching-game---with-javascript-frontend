package com.memorygame.repository;

import com.memorygame.model.PerformanceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PerformanceHistoryRepository extends JpaRepository<PerformanceHistory, Long> {

    List<PerformanceHistory> findByPlayerIdOrderBySessionDateDesc(Long playerId);
}