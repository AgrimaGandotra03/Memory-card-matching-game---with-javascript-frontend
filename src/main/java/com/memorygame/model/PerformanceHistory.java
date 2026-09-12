package com.memorygame.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Immutable cognitive-performance snapshot written when a game is completed. */
@Entity
@Table(name = "performance_history")
public class PerformanceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private User player;

    @Column(nullable = false)
    private LocalDateTime sessionDate;

    @Column(nullable = false)
    private int finalScore;

    @Column(nullable = false)
    private double accuracyPercent;

    @Column(nullable = false)
    private long totalTimeSeconds;

    @Column(nullable = false)
    private int mistakeCount;

    @Column(nullable = false)
    private int maxStreak;

    public PerformanceHistory() {}

    public PerformanceHistory(User player, LocalDateTime sessionDate, int finalScore,
                              double accuracyPercent, long totalTimeSeconds,
                              int mistakeCount, int maxStreak) {
        this.player = player;
        this.sessionDate = sessionDate;
        this.finalScore = finalScore;
        this.accuracyPercent = accuracyPercent;
        this.totalTimeSeconds = totalTimeSeconds;
        this.mistakeCount = mistakeCount;
        this.maxStreak = maxStreak;
    }

    public Long getId() { return id; }

    public User getPlayer() { return player; }
    public void setPlayer(User player) { this.player = player; }

    public LocalDateTime getSessionDate() { return sessionDate; }
    public void setSessionDate(LocalDateTime sessionDate) { this.sessionDate = sessionDate; }

    public int getFinalScore() { return finalScore; }
    public void setFinalScore(int finalScore) { this.finalScore = finalScore; }

    public double getAccuracyPercent() { return accuracyPercent; }
    public void setAccuracyPercent(double accuracyPercent) { this.accuracyPercent = accuracyPercent; }

    public long getTotalTimeSeconds() { return totalTimeSeconds; }
    public void setTotalTimeSeconds(long totalTimeSeconds) { this.totalTimeSeconds = totalTimeSeconds; }

    public int getMistakeCount() { return mistakeCount; }
    public void setMistakeCount(int mistakeCount) { this.mistakeCount = mistakeCount; }

    public int getMaxStreak() { return maxStreak; }
    public void setMaxStreak(int maxStreak) { this.maxStreak = maxStreak; }
}