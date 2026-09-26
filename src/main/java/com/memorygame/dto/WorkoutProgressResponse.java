package com.memorygame.dto;

import java.util.List;
import java.util.Map;

public class WorkoutProgressResponse {
    private int totalWorkoutsCompleted;
    private int currentStreakDays;
    private Map<String, Integer> personalBestBySkill;
    private List<WorkoutHistoryEntryDto> recentHistory;

    public int getTotalWorkoutsCompleted() { return totalWorkoutsCompleted; }
    public void setTotalWorkoutsCompleted(int totalWorkoutsCompleted) { this.totalWorkoutsCompleted = totalWorkoutsCompleted; }

    public int getCurrentStreakDays() { return currentStreakDays; }
    public void setCurrentStreakDays(int currentStreakDays) { this.currentStreakDays = currentStreakDays; }

    public Map<String, Integer> getPersonalBestBySkill() { return personalBestBySkill; }
    public void setPersonalBestBySkill(Map<String, Integer> personalBestBySkill) { this.personalBestBySkill = personalBestBySkill; }

    public List<WorkoutHistoryEntryDto> getRecentHistory() { return recentHistory; }
    public void setRecentHistory(List<WorkoutHistoryEntryDto> recentHistory) { this.recentHistory = recentHistory; }
}
