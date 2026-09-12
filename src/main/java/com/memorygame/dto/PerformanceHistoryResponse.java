package com.memorygame.dto;

import java.time.LocalDateTime;

public class PerformanceHistoryResponse {

    private Long id;
    private Long playerId;
    private LocalDateTime sessionDate;
    private int finalScore;
    private double accuracyPercent;
    private long totalTimeSeconds;
    private int mistakeCount;
    private int maxStreak;
    private double concentrationScore;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPlayerId() { return playerId; }
    public void setPlayerId(Long playerId) { this.playerId = playerId; }
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
    public double getConcentrationScore() { return concentrationScore; }
    public void setConcentrationScore(double concentrationScore) { this.concentrationScore = concentrationScore; }
}