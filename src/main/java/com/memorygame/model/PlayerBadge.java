package com.memorygame.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "player_badges",
       uniqueConstraints = @UniqueConstraint(name = "uk_player_badge", columnNames = {"player_id", "badge_code"}))
public class PlayerBadge {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private User player;
    @Column(name = "badge_code", nullable = false, length = 40)
    private String badgeCode;
    @Column(nullable = false)
    private LocalDateTime earnedAt;

    public PlayerBadge() {}
    public PlayerBadge(User player, String badgeCode) {
        this.player = player; this.badgeCode = badgeCode; this.earnedAt = LocalDateTime.now();
    }
    public Long getId() { return id; }
    public User getPlayer() { return player; }
    public String getBadgeCode() { return badgeCode; }
    public LocalDateTime getEarnedAt() { return earnedAt; }
}
