package com.memorygame.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Extended profile info for each player.
 * Has a one-to-one relationship with User.
 */
@Entity
@Table(name = "player_profiles")
public class PlayerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // One user → one profile
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(length = 50)
    private String displayName;

    // e.g. "animals", "fruits", "flags" — whatever themes you add later
    @Column(length = 30)
    private String preferredTheme;

    @Column(nullable = false)
    private int totalGamesPlayed = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ---------- lifecycle ----------

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ---------- constructors ----------

    public PlayerProfile() {}

    public PlayerProfile(User user) {
        this.user = user;
        this.displayName = user.getUsername(); // sensible default
    }

    // ---------- getters & setters ----------

    public Long getId() { return id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getPreferredTheme() { return preferredTheme; }
    public void setPreferredTheme(String preferredTheme) { this.preferredTheme = preferredTheme; }

    public int getTotalGamesPlayed() { return totalGamesPlayed; }
    public void setTotalGamesPlayed(int totalGamesPlayed) { this.totalGamesPlayed = totalGamesPlayed; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
