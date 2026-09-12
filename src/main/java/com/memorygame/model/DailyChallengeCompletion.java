package com.memorygame.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_challenge_completions",
       uniqueConstraints = @UniqueConstraint(name = "uk_daily_player_day", columnNames = {"player_id", "challenge_date"}))
public class DailyChallengeCompletion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private User player;
    @Column(name = "challenge_date", nullable = false)
    private LocalDate challengeDate;
    @Column(nullable = false)
    private int score;
    @Column(nullable = false)
    private LocalDateTime completedAt;

    public DailyChallengeCompletion() {}
    public DailyChallengeCompletion(User player, LocalDate challengeDate, int score) {
        this.player = player; this.challengeDate = challengeDate; this.score = score;
        this.completedAt = LocalDateTime.now();
    }
    public Long getId() { return id; }
    public User getPlayer() { return player; }
    public LocalDate getChallengeDate() { return challengeDate; }
    public int getScore() { return score; }
    public LocalDateTime getCompletedAt() { return completedAt; }
}
