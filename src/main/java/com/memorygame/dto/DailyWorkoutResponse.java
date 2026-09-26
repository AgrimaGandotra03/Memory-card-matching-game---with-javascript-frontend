package com.memorygame.dto;

import java.time.LocalDate;

public class DailyWorkoutResponse {
    private LocalDate date;
    private String skill;
    private String skillLabel;
    private String description;
    private String level;
    private int levelStars;
    private int estimatedMinutes;
    private boolean weekendBonus;
    private boolean completedToday;
    private Integer todayScore;
    private Integer personalBest;
    private int currentStreakDays;
    private WorkoutChallengeDto challenge;

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getSkill() { return skill; }
    public void setSkill(String skill) { this.skill = skill; }

    public String getSkillLabel() { return skillLabel; }
    public void setSkillLabel(String skillLabel) { this.skillLabel = skillLabel; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public int getLevelStars() { return levelStars; }
    public void setLevelStars(int levelStars) { this.levelStars = levelStars; }

    public int getEstimatedMinutes() { return estimatedMinutes; }
    public void setEstimatedMinutes(int estimatedMinutes) { this.estimatedMinutes = estimatedMinutes; }

    public boolean isWeekendBonus() { return weekendBonus; }
    public void setWeekendBonus(boolean weekendBonus) { this.weekendBonus = weekendBonus; }

    public boolean isCompletedToday() { return completedToday; }
    public void setCompletedToday(boolean completedToday) { this.completedToday = completedToday; }

    public Integer getTodayScore() { return todayScore; }
    public void setTodayScore(Integer todayScore) { this.todayScore = todayScore; }

    public Integer getPersonalBest() { return personalBest; }
    public void setPersonalBest(Integer personalBest) { this.personalBest = personalBest; }

    public int getCurrentStreakDays() { return currentStreakDays; }
    public void setCurrentStreakDays(int currentStreakDays) { this.currentStreakDays = currentStreakDays; }

    public WorkoutChallengeDto getChallenge() { return challenge; }
    public void setChallenge(WorkoutChallengeDto challenge) { this.challenge = challenge; }
}
