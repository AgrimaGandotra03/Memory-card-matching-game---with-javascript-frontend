package com.memorygame.dto;

import java.time.LocalDate;

public class WorkoutHistoryEntryDto {
    private LocalDate date;
    private String skill;
    private String skillLabel;
    private String level;
    private int score;
    private double accuracyPercent;
    private int timeTakenSeconds;
    private boolean personalBest;

    public WorkoutHistoryEntryDto() {}

    public WorkoutHistoryEntryDto(LocalDate date, String skill, String skillLabel, String level,
                                   int score, double accuracyPercent, int timeTakenSeconds, boolean personalBest) {
        this.date = date;
        this.skill = skill;
        this.skillLabel = skillLabel;
        this.level = level;
        this.score = score;
        this.accuracyPercent = accuracyPercent;
        this.timeTakenSeconds = timeTakenSeconds;
        this.personalBest = personalBest;
    }

    public LocalDate getDate() { return date; }
    public String getSkill() { return skill; }
    public String getSkillLabel() { return skillLabel; }
    public String getLevel() { return level; }
    public int getScore() { return score; }
    public double getAccuracyPercent() { return accuracyPercent; }
    public int getTimeTakenSeconds() { return timeTakenSeconds; }
    public boolean isPersonalBest() { return personalBest; }
}
