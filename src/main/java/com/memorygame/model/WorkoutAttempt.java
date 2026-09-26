package com.memorygame.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Persists one completed (or abandoned) Daily Brain Workout attempt.
 * One row per submission; a player may retry the same day's skill more than once,
 * so history keeps every attempt while "personal best" is derived by the service.
 */
@Entity
@Table(name = "workout_attempts")
public class WorkoutAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private User player;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkoutSkill skill;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private WorkoutLevel level;

    /** Calendar day this workout belongs to (server date, UTC) — not necessarily "today" if retried later. */
    @Column(nullable = false)
    private LocalDate attemptDate;

    @Column(nullable = false)
    private int correctCount;

    @Column(nullable = false)
    private int incorrectCount;

    @Column(nullable = false)
    private double accuracyPercent;

    @Column(nullable = false)
    private int timeTakenSeconds;

    /** Final 0-100 workout score computed server-side from accuracy + speed. */
    @Column(nullable = false)
    private int score;

    private Long avgReactionTimeMillis;

    /** Sequence Memory: longest sequence successfully reproduced in this attempt. */
    private Integer maxSequenceLength;

    /** Delayed Recall: accuracy measured immediately after encoding, before the delay. */
    private Double immediateAccuracyPercent;

    /** Delayed Recall: accuracy measured after the delay — the metric that matters most. */
    private Double delayedAccuracyPercent;

    private Integer retries;

    @Column(nullable = false)
    private boolean personalBest = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public WorkoutAttempt() {}

    // ---------- getters & setters ----------

    public Long getId() { return id; }

    public User getPlayer() { return player; }
    public void setPlayer(User player) { this.player = player; }

    public WorkoutSkill getSkill() { return skill; }
    public void setSkill(WorkoutSkill skill) { this.skill = skill; }

    public WorkoutLevel getLevel() { return level; }
    public void setLevel(WorkoutLevel level) { this.level = level; }

    public LocalDate getAttemptDate() { return attemptDate; }
    public void setAttemptDate(LocalDate attemptDate) { this.attemptDate = attemptDate; }

    public int getCorrectCount() { return correctCount; }
    public void setCorrectCount(int correctCount) { this.correctCount = correctCount; }

    public int getIncorrectCount() { return incorrectCount; }
    public void setIncorrectCount(int incorrectCount) { this.incorrectCount = incorrectCount; }

    public double getAccuracyPercent() { return accuracyPercent; }
    public void setAccuracyPercent(double accuracyPercent) { this.accuracyPercent = accuracyPercent; }

    public int getTimeTakenSeconds() { return timeTakenSeconds; }
    public void setTimeTakenSeconds(int timeTakenSeconds) { this.timeTakenSeconds = timeTakenSeconds; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public Long getAvgReactionTimeMillis() { return avgReactionTimeMillis; }
    public void setAvgReactionTimeMillis(Long avgReactionTimeMillis) { this.avgReactionTimeMillis = avgReactionTimeMillis; }

    public Integer getMaxSequenceLength() { return maxSequenceLength; }
    public void setMaxSequenceLength(Integer maxSequenceLength) { this.maxSequenceLength = maxSequenceLength; }

    public Double getImmediateAccuracyPercent() { return immediateAccuracyPercent; }
    public void setImmediateAccuracyPercent(Double immediateAccuracyPercent) { this.immediateAccuracyPercent = immediateAccuracyPercent; }

    public Double getDelayedAccuracyPercent() { return delayedAccuracyPercent; }
    public void setDelayedAccuracyPercent(Double delayedAccuracyPercent) { this.delayedAccuracyPercent = delayedAccuracyPercent; }

    public Integer getRetries() { return retries; }
    public void setRetries(Integer retries) { this.retries = retries; }

    public boolean isPersonalBest() { return personalBest; }
    public void setPersonalBest(boolean personalBest) { this.personalBest = personalBest; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
