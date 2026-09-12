package com.memorygame.dto;

import com.memorygame.model.Difficulty;
import com.memorygame.model.GameMode;
import java.time.LocalDate;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for POST /api/game/start
 *
 * Example:
 *   { "userId": 1, "difficulty": "MEDIUM", "theme": "animals" }
 */
public class StartGameRequest {

    @NotNull(message = "userId is required")
    private Long userId;

    @NotNull(message = "difficulty is required — EASY, MEDIUM, or HARD")
    private Difficulty difficulty;

    /** Falls back to "animals" if omitted or unrecognised. */
    private String theme = "animals";

    /** Optional cognitive-training mode; omitted requests remain standard games. */
    private boolean focusMode = false;

    /** Defaults to CLASSIC for existing clients. */
    private GameMode mode = GameMode.CLASSIC;
    private boolean dailyChallenge = false;
    private LocalDate dailyChallengeDate;

    // ---------- getters & setters ----------

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Difficulty getDifficulty() { return difficulty; }
    public void setDifficulty(Difficulty difficulty) { this.difficulty = difficulty; }

    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }

    public boolean isFocusMode() { return focusMode; }
    public void setFocusMode(boolean focusMode) { this.focusMode = focusMode; }

    public GameMode getMode() { return mode == null ? GameMode.CLASSIC : mode; }
    public void setMode(GameMode mode) { this.mode = mode; }
    public boolean isDailyChallenge() { return dailyChallenge; }
    public void setDailyChallenge(boolean dailyChallenge) { this.dailyChallenge = dailyChallenge; }
    public LocalDate getDailyChallengeDate() { return dailyChallengeDate; }
    public void setDailyChallengeDate(LocalDate dailyChallengeDate) { this.dailyChallengeDate = dailyChallengeDate; }
}
