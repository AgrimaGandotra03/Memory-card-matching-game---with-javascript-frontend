package com.memorygame.dto;

public class WorkoutAttemptResponse {
    private int score;
    private double accuracyPercent;
    private boolean personalBest;
    private Integer previousBest;
    private int currentStreakDays;
    private String message;

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public double getAccuracyPercent() { return accuracyPercent; }
    public void setAccuracyPercent(double accuracyPercent) { this.accuracyPercent = accuracyPercent; }

    public boolean isPersonalBest() { return personalBest; }
    public void setPersonalBest(boolean personalBest) { this.personalBest = personalBest; }

    public Integer getPreviousBest() { return previousBest; }
    public void setPreviousBest(Integer previousBest) { this.previousBest = previousBest; }

    public int getCurrentStreakDays() { return currentStreakDays; }
    public void setCurrentStreakDays(int currentStreakDays) { this.currentStreakDays = currentStreakDays; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
