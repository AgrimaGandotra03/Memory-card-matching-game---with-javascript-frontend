package com.memorygame.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalDate;

/**
 * Persists one game session for a user.
 *
 * Timer design (server-side source of truth):
 *   - startedAt:          set when the game begins or resumes
 *   - pausedAt:           set when the game is paused; cleared on resume
 *   - totalPausedSeconds: accumulated pause time; incremented on each resume
 *   - elapsedSeconds (computed): now − startedAt − totalPausedSeconds
 *
 * Board design:
 *   boardStateJson stores the full List<CardState> as a single TEXT column.
 *   This avoids a join table for a prototype where boards are ≤ 64 cards.
 *
 * Flip-state tracking:
 *   firstFlippedCardId tracks which card was flipped first in the current turn.
 *   -1 means no card is currently waiting for its pair.
 *   This is stored as a column (not in the JSON) so it's always cleanly persisted.
 */
@Entity
@Table(name = "game_sessions")
public class GameSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Difficulty difficulty;

    @Column(length = 30)
    private String theme;

    /** Full JSON-serialized List<CardState> for the board. */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String boardStateJson;

    /** Total number of two-card flip attempts made by the player. */
    @Column(nullable = false)
    private int moves = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private GameStatus status = GameStatus.ACTIVE;

    @Column(nullable = false)
    private int hintsUsed = 0;

    /** Final score — populated only when status transitions to WON. */
    @Column(nullable = false)
    private int score = 0;

    /** Average active-play time per completed two-card move, in milliseconds. */
    private Long averageReactionTimeMillis = 0L;

    /** Number of two-card moves that did not produce a match. */
    private Integer mistakeCount = 0;

    /** Number of consecutive matching moves at the current point in the game. */
    private Integer currentConsecutiveMatchStreak = 0;

    /** Highest consecutive matching-move streak reached in this session. */
    private Integer maxConsecutiveMatchStreak = 0;

    /** Placeholder concentration score on a 0-100 scale. */
    private Double concentrationScore = 100.0;

    /** Server-owned end of the initial memory preview window. */
    private Instant previewEndsAt;

    /** Server timestamp of the first card in the current two-card attempt. */
    private Instant firstFlipAt;

    /** Server-owned deadline for the current two-card attempt. */
    private Instant moveDeadlineAt;

    /** Enables the stricter focus-mode move timer. */
    private Boolean focusMode = false;

    /** Adaptive per-move limit in seconds. */
    private Integer moveTimeLimitSeconds = 0;

    /** Correct matches divided by completed two-card attempts, on a 0-100 scale. */
    private Double accuracyPercent = 0.0;

    /** Selected high-level game mode. */
    @Enumerated(EnumType.STRING)
    @Column(length = 24)
    private GameMode mode = GameMode.CLASSIC;

    /** Server-owned end of a timed challenge. */
    private Instant gameDeadlineAt;

    /** Fixed duration for a timed challenge, in seconds. */
    private Integer totalTimeLimitSeconds = 0;

    /** Current level within a progressive session. */
    private Integer level = 1;

    /** Score accumulated across completed progressive levels. */
    private Integer cumulativeScore = 0;

    /** Ordered card IDs for sequence mode, stored as JSON. */
    @Column(columnDefinition = "TEXT")
    private String sequenceOrderJson;

    /** Start of sequence playback — lets the client compute per-card reveal timing. */
    private Instant sequencePlaybackStartedAt;

    /** End of sequence playback before replay input is accepted. */
    private Instant sequencePlaybackEndsAt;

    /** Next sequence index the player must reproduce. */
    private Integer sequencePosition = 0;

    /** Prevents duplicate history rows when a terminal session is polled. */
    private Boolean performanceRecorded = false;

    private Long performanceHistoryId;

    private Boolean dailyChallenge = false;
    private LocalDate dailyChallengeDate;

    // ── Server-side timer fields ──────────────────────────────────────────────

    /** When this session was last started (or re-started after a restart). */
    private Instant startedAt;

    /** Set when paused; null when active. */
    private Instant pausedAt;

    /** Sum of all pause durations accumulated so far. */
    @Column(nullable = false)
    private long totalPausedSeconds = 0;

    // ── Flip-turn state ───────────────────────────────────────────────────────

    /**
     * Card ID of the first card flipped in the current turn.
     * -1 means no card is currently awaiting a partner flip.
     */
    @Column(nullable = false)
    private int firstFlippedCardId = -1;

    // ── Timestamps ───────────────────────────────────────────────────────────

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ---------- constructors ----------

    public GameSession() {}

    // ---------- getters & setters ----------

    public Long getId() { return id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Difficulty getDifficulty() { return difficulty; }
    public void setDifficulty(Difficulty difficulty) { this.difficulty = difficulty; }

    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }

    public String getBoardStateJson() { return boardStateJson; }
    public void setBoardStateJson(String boardStateJson) { this.boardStateJson = boardStateJson; }

    public int getMoves() { return moves; }
    public void setMoves(int moves) { this.moves = moves; }

    public GameStatus getStatus() { return status; }
    public void setStatus(GameStatus status) { this.status = status; }

    public int getHintsUsed() { return hintsUsed; }
    public void setHintsUsed(int hintsUsed) { this.hintsUsed = hintsUsed; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public Long getAverageReactionTimeMillis() {
        return averageReactionTimeMillis == null ? 0L : averageReactionTimeMillis;
    }
    public void setAverageReactionTimeMillis(Long averageReactionTimeMillis) {
        this.averageReactionTimeMillis = averageReactionTimeMillis;
    }

    public Integer getMistakeCount() { return mistakeCount == null ? 0 : mistakeCount; }
    public void setMistakeCount(Integer mistakeCount) { this.mistakeCount = mistakeCount; }

    public Integer getCurrentConsecutiveMatchStreak() {
        return currentConsecutiveMatchStreak == null ? 0 : currentConsecutiveMatchStreak;
    }
    public void setCurrentConsecutiveMatchStreak(Integer currentConsecutiveMatchStreak) {
        this.currentConsecutiveMatchStreak = currentConsecutiveMatchStreak;
    }

    public Integer getMaxConsecutiveMatchStreak() {
        return maxConsecutiveMatchStreak == null ? 0 : maxConsecutiveMatchStreak;
    }
    public void setMaxConsecutiveMatchStreak(Integer maxConsecutiveMatchStreak) {
        this.maxConsecutiveMatchStreak = maxConsecutiveMatchStreak;
    }

    public Double getConcentrationScore() {
        return concentrationScore == null ? 0.0 : concentrationScore;
    }
    public void setConcentrationScore(Double concentrationScore) {
        this.concentrationScore = concentrationScore;
    }

    public Instant getPreviewEndsAt() { return previewEndsAt; }
    public void setPreviewEndsAt(Instant previewEndsAt) { this.previewEndsAt = previewEndsAt; }

    public Instant getFirstFlipAt() { return firstFlipAt; }
    public void setFirstFlipAt(Instant firstFlipAt) { this.firstFlipAt = firstFlipAt; }

    public Instant getMoveDeadlineAt() { return moveDeadlineAt; }
    public void setMoveDeadlineAt(Instant moveDeadlineAt) { this.moveDeadlineAt = moveDeadlineAt; }

    public boolean isFocusMode() { return Boolean.TRUE.equals(focusMode); }
    public void setFocusMode(Boolean focusMode) { this.focusMode = focusMode; }

    public Integer getMoveTimeLimitSeconds() {
        return moveTimeLimitSeconds == null ? 0 : moveTimeLimitSeconds;
    }
    public void setMoveTimeLimitSeconds(Integer moveTimeLimitSeconds) {
        this.moveTimeLimitSeconds = moveTimeLimitSeconds;
    }

    public Double getAccuracyPercent() {
        return accuracyPercent == null ? 0.0 : accuracyPercent;
    }
    public void setAccuracyPercent(Double accuracyPercent) { this.accuracyPercent = accuracyPercent; }

    public GameMode getMode() { return mode == null ? GameMode.CLASSIC : mode; }
    public void setMode(GameMode mode) { this.mode = mode; }

    public Instant getGameDeadlineAt() { return gameDeadlineAt; }
    public void setGameDeadlineAt(Instant gameDeadlineAt) { this.gameDeadlineAt = gameDeadlineAt; }

    public Integer getTotalTimeLimitSeconds() {
        return totalTimeLimitSeconds == null ? 0 : totalTimeLimitSeconds;
    }
    public void setTotalTimeLimitSeconds(Integer totalTimeLimitSeconds) {
        this.totalTimeLimitSeconds = totalTimeLimitSeconds;
    }

    public Integer getLevel() { return level == null ? 1 : level; }
    public void setLevel(Integer level) { this.level = level; }

    public Integer getCumulativeScore() { return cumulativeScore == null ? 0 : cumulativeScore; }
    public void setCumulativeScore(Integer cumulativeScore) { this.cumulativeScore = cumulativeScore; }

    public String getSequenceOrderJson() { return sequenceOrderJson; }
    public void setSequenceOrderJson(String sequenceOrderJson) { this.sequenceOrderJson = sequenceOrderJson; }

    public Instant getSequencePlaybackStartedAt() { return sequencePlaybackStartedAt; }
    public void setSequencePlaybackStartedAt(Instant sequencePlaybackStartedAt) {
        this.sequencePlaybackStartedAt = sequencePlaybackStartedAt;
    }

    public Instant getSequencePlaybackEndsAt() { return sequencePlaybackEndsAt; }
    public void setSequencePlaybackEndsAt(Instant sequencePlaybackEndsAt) {
        this.sequencePlaybackEndsAt = sequencePlaybackEndsAt;
    }

    public Integer getSequencePosition() { return sequencePosition == null ? 0 : sequencePosition; }
    public void setSequencePosition(Integer sequencePosition) { this.sequencePosition = sequencePosition; }

    public boolean isPerformanceRecorded() { return Boolean.TRUE.equals(performanceRecorded); }
    public void setPerformanceRecorded(Boolean performanceRecorded) { this.performanceRecorded = performanceRecorded; }

    public Long getPerformanceHistoryId() { return performanceHistoryId; }
    public void setPerformanceHistoryId(Long performanceHistoryId) { this.performanceHistoryId = performanceHistoryId; }

    public boolean isDailyChallenge() { return Boolean.TRUE.equals(dailyChallenge); }
    public void setDailyChallenge(Boolean dailyChallenge) { this.dailyChallenge = dailyChallenge; }
    public LocalDate getDailyChallengeDate() { return dailyChallengeDate; }
    public void setDailyChallengeDate(LocalDate dailyChallengeDate) { this.dailyChallengeDate = dailyChallengeDate; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getPausedAt() { return pausedAt; }
    public void setPausedAt(Instant pausedAt) { this.pausedAt = pausedAt; }

    public long getTotalPausedSeconds() { return totalPausedSeconds; }
    public void setTotalPausedSeconds(long totalPausedSeconds) { this.totalPausedSeconds = totalPausedSeconds; }

    public int getFirstFlippedCardId() { return firstFlippedCardId; }
    public void setFirstFlippedCardId(int firstFlippedCardId) { this.firstFlippedCardId = firstFlippedCardId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
