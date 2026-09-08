package com.memorygame.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Immutable record written whenever a player wins a game.
 * Used for the score history / leaderboard view.
 */
@Entity
@Table(name = "score_records")
public class ScoreRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Difficulty difficulty;

    @Column(nullable = false)
    private int moves;

    @Column(nullable = false)
    private long timeTakenSeconds;

    @Column(nullable = false)
    private int score;

    @Column(nullable = false)
    private int hintsUsed;

    @Column(nullable = false)
    private LocalDateTime playedAt;

    // ---------- constructors ----------

    public ScoreRecord() {}

    public ScoreRecord(User user, Difficulty difficulty, int moves,
                       long timeTakenSeconds, int score, int hintsUsed) {
        this.user = user;
        this.difficulty = difficulty;
        this.moves = moves;
        this.timeTakenSeconds = timeTakenSeconds;
        this.score = score;
        this.hintsUsed = hintsUsed;
        this.playedAt = LocalDateTime.now();
    }

    // ---------- getters & setters ----------

    public Long getId() { return id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Difficulty getDifficulty() { return difficulty; }
    public void setDifficulty(Difficulty difficulty) { this.difficulty = difficulty; }

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
