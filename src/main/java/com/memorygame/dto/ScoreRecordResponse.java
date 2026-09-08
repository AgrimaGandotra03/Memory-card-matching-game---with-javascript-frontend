package com.memorygame.dto;

import java.time.LocalDateTime;

/**
 * Response element for GET /api/scores/{userId}
 *
 * Represents a single completed game in a player's history.
 */
public class ScoreRecordResponse {

    private Long id;
    private Long userId;
    private String username;
    private String difficulty;
    private int moves;
    private long timeTakenSeconds;
    private int score;
    private int hintsUsed;
    private LocalDateTime playedAt;

    // ---------- getters & setters ----------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public int getMoves() { return moves; }
    public void setMoves(int moves) { this.moves = moves; }

    public long getTimeTakenSeconds() { return timeTakenSeconds; }
    public void setTimeTakenSeconds(long timeTakenSeconds) { this.timeTakenSeconds = timeTakenSeconds; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public int getHintsUsed() { return hintsUsed; }
    public void setHintsUsed(int hintsUsed) { this.hintsUsed = hintsUsed; }

    public LocalDateTime getPlayedAt() { return playedAt; }
    public void setPlayedAt(LocalDateTime playedAt) { this.playedAt = playedAt; }
}
