package com.memorygame.dto;

public class SessionPerformanceReport {

    private int score;
    private double accuracyPercent;
    private long timeTakenSeconds;
    private int mistakeCount;
    private int maxStreak;
    private double concentrationScore;
    private double averageScore;
    private double averageAccuracyPercent;
    private double averageTimeTakenSeconds;
    private double averageConcentrationScore;

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public double getAccuracyPercent() { return accuracyPercent; }
    public void setAccuracyPercent(double accuracyPercent) { this.accuracyPercent = accuracyPercent; }
    public long getTimeTakenSeconds() { return timeTakenSeconds; }
    public void setTimeTakenSeconds(long timeTakenSeconds) { this.timeTakenSeconds = timeTakenSeconds; }
    public int getMistakeCount() { return mistakeCount; }
    public void setMistakeCount(int mistakeCount) { this.mistakeCount = mistakeCount; }
    public int getMaxStreak() { return maxStreak; }
    public void setMaxStreak(int maxStreak) { this.maxStreak = maxStreak; }
    public double getConcentrationScore() { return concentrationScore; }
    public void setConcentrationScore(double concentrationScore) { this.concentrationScore = concentrationScore; }
    public double getAverageScore() { return averageScore; }
    public void setAverageScore(double averageScore) { this.averageScore = averageScore; }
    public double getAverageAccuracyPercent() { return averageAccuracyPercent; }
    public void setAverageAccuracyPercent(double averageAccuracyPercent) { this.averageAccuracyPercent = averageAccuracyPercent; }
    public double getAverageTimeTakenSeconds() { return averageTimeTakenSeconds; }
    public void setAverageTimeTakenSeconds(double averageTimeTakenSeconds) { this.averageTimeTakenSeconds = averageTimeTakenSeconds; }
    public double getAverageConcentrationScore() { return averageConcentrationScore; }
    public void setAverageConcentrationScore(double averageConcentrationScore) { this.averageConcentrationScore = averageConcentrationScore; }
}