package com.memorygame.dto;

import java.time.Instant;
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

    private double accuracyPercent;
    private int currentStreak;
    private int maxStreak;
    private long averageReactionTimeMillis;
    private double concentrationScore;
    private boolean previewing;
    private Instant previewEndsAt;
    private boolean focusMode;
    private int moveTimeLimitSeconds;
    private Instant moveDeadlineAt;
    private String mode;
    private Instant gameDeadlineAt;
    private long timeRemainingSeconds;
    private int level;
    private int cumulativeScore;
    private boolean sequencePlaybackActive;
    private Instant sequencePlaybackStartedAt;
    private Instant sequencePlaybackEndsAt;
    private List<Integer> sequencePlaybackCardIds;
    private int sequenceExpectedPosition;
    private boolean levelCompleted;
    private SessionPerformanceReport performanceReport;

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

    /**
     * Card ID(s) involved in this move, with symbolKey force-revealed in
     * `board` for those IDs even if their flipped/matched flags say hidden.
     * Needed because on NO_MATCH the server flips both cards back down
     * (and re-masks them) before the response is built — without this,
     * the client has no way to show what the second card actually was.
     */
    private List<Integer> revealedThisMove;

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

    public double getAccuracyPercent() { return accuracyPercent; }
    public void setAccuracyPercent(double accuracyPercent) { this.accuracyPercent = accuracyPercent; }

    public int getCurrentStreak() { return currentStreak; }
    public void setCurrentStreak(int currentStreak) { this.currentStreak = currentStreak; }

    public int getMaxStreak() { return maxStreak; }
    public void setMaxStreak(int maxStreak) { this.maxStreak = maxStreak; }

    public long getAverageReactionTimeMillis() { return averageReactionTimeMillis; }
    public void setAverageReactionTimeMillis(long averageReactionTimeMillis) {
        this.averageReactionTimeMillis = averageReactionTimeMillis;
    }

    public double getConcentrationScore() { return concentrationScore; }
    public void setConcentrationScore(double concentrationScore) {
        this.concentrationScore = concentrationScore;
    }

    public boolean isPreviewing() { return previewing; }
    public void setPreviewing(boolean previewing) { this.previewing = previewing; }

    public Instant getPreviewEndsAt() { return previewEndsAt; }
    public void setPreviewEndsAt(Instant previewEndsAt) { this.previewEndsAt = previewEndsAt; }

    public boolean isFocusMode() { return focusMode; }
    public void setFocusMode(boolean focusMode) { this.focusMode = focusMode; }

    public int getMoveTimeLimitSeconds() { return moveTimeLimitSeconds; }
    public void setMoveTimeLimitSeconds(int moveTimeLimitSeconds) {
        this.moveTimeLimitSeconds = moveTimeLimitSeconds;
    }

    public Instant getMoveDeadlineAt() { return moveDeadlineAt; }
    public void setMoveDeadlineAt(Instant moveDeadlineAt) { this.moveDeadlineAt = moveDeadlineAt; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public Instant getGameDeadlineAt() { return gameDeadlineAt; }
    public void setGameDeadlineAt(Instant gameDeadlineAt) { this.gameDeadlineAt = gameDeadlineAt; }

    public long getTimeRemainingSeconds() { return timeRemainingSeconds; }
    public void setTimeRemainingSeconds(long timeRemainingSeconds) {
        this.timeRemainingSeconds = timeRemainingSeconds;
    }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public int getCumulativeScore() { return cumulativeScore; }
    public void setCumulativeScore(int cumulativeScore) { this.cumulativeScore = cumulativeScore; }

    public boolean isSequencePlaybackActive() { return sequencePlaybackActive; }
    public void setSequencePlaybackActive(boolean sequencePlaybackActive) {
        this.sequencePlaybackActive = sequencePlaybackActive;
    }

    public Instant getSequencePlaybackStartedAt() { return sequencePlaybackStartedAt; }
    public void setSequencePlaybackStartedAt(Instant sequencePlaybackStartedAt) {
        this.sequencePlaybackStartedAt = sequencePlaybackStartedAt;
    }

    public Instant getSequencePlaybackEndsAt() { return sequencePlaybackEndsAt; }
    public void setSequencePlaybackEndsAt(Instant sequencePlaybackEndsAt) {
        this.sequencePlaybackEndsAt = sequencePlaybackEndsAt;
    }

    public List<Integer> getSequencePlaybackCardIds() { return sequencePlaybackCardIds; }
    public void setSequencePlaybackCardIds(List<Integer> sequencePlaybackCardIds) {
        this.sequencePlaybackCardIds = sequencePlaybackCardIds;
    }

    public int getSequenceExpectedPosition() { return sequenceExpectedPosition; }
    public void setSequenceExpectedPosition(int sequenceExpectedPosition) {
        this.sequenceExpectedPosition = sequenceExpectedPosition;
    }

    public boolean isLevelCompleted() { return levelCompleted; }
    public void setLevelCompleted(boolean levelCompleted) { this.levelCompleted = levelCompleted; }

    public SessionPerformanceReport getPerformanceReport() { return performanceReport; }
    public void setPerformanceReport(SessionPerformanceReport performanceReport) {
        this.performanceReport = performanceReport;
    }

    public List<CardDto> getBoard() { return board; }
    public void setBoard(List<CardDto> board) { this.board = board; }

    public String getFlipResult() { return flipResult; }
    public void setFlipResult(String flipResult) { this.flipResult = flipResult; }

    public boolean isWonGame() { return wonGame; }
    public void setWonGame(boolean wonGame) { this.wonGame = wonGame; }

    public List<Integer> getRevealedThisMove() { return revealedThisMove; }
    public void setRevealedThisMove(List<Integer> revealedThisMove) { this.revealedThisMove = revealedThisMove; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
