package com.memorygame.dto;

import java.util.List;

/**
 * Standard response for all game session endpoints.
 *
 * flipResult and wonGame are only populated on POST /{sessionId}/flip responses;
 * they are null/false on all other endpoints.
 */
public class GameSessionResponse {

    private Long sessionId;
    private Long userId;
    private String difficulty;   // "EASY" | "MEDIUM" | "HARD"
    private String theme;        // e.g. "animals"
    private int moves;           // total two-card flip attempts so far
    private long elapsedSeconds; // server-computed; never trusted from client
    private String status;       // "ACTIVE" | "PAUSED" | "WON" | "LOST"
    private int hintsUsed;
    private int score;           // 0 until the game is WON

    /** The board in display order; symbolKey is null for hidden cards. */
    private List<CardDto> board;

    // ── Flip-specific fields (populated only on flipCard responses) ──────────

    /**
     * "FIRST_FLIP"  → first card of the turn flipped; waiting for the second.
     * "MATCH"       → both cards matched; they stay face-up.
     * "NO_MATCH"    → no match; both cards were flipped back to face-down.
     * null          → not a flip response.
     */
    private String flipResult;

    /** true when this flip completed the last pair and won the game. */
    private boolean wonGame;

    /** Human-readable status message for UI display or debugging. */
    private String message;

    // ---------- getters & setters ----------

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }

    public int getMoves() { return moves; }
    public void setMoves(int moves) { this.moves = moves; }

    public long getElapsedSeconds() { return elapsedSeconds; }
    public void setElapsedSeconds(long elapsedSeconds) { this.elapsedSeconds = elapsedSeconds; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getHintsUsed() { return hintsUsed; }
    public void setHintsUsed(int hintsUsed) { this.hintsUsed = hintsUsed; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public List<CardDto> getBoard() { return board; }
    public void setBoard(List<CardDto> board) { this.board = board; }

    public String getFlipResult() { return flipResult; }
    public void setFlipResult(String flipResult) { this.flipResult = flipResult; }

    public boolean isWonGame() { return wonGame; }
    public void setWonGame(boolean wonGame) { this.wonGame = wonGame; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
