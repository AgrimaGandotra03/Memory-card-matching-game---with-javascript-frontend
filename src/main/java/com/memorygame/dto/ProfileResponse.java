package com.memorygame.dto;

/**
 * Returned by GET /api/profile/{userId}.
 * Contains all the info the frontend needs to render a profile page.
 */
public class ProfileResponse {

    private Long userId;
    private String username;
    private String displayName;
    private String preferredTheme;
    private int totalGamesPlayed;
    private String memberSince; // human-readable date string

    // ---------- constructors ----------

    public ProfileResponse() {}

    public ProfileResponse(Long userId, String username, String displayName,
                           String preferredTheme, int totalGamesPlayed, String memberSince) {
        this.userId = userId;
        this.username = username;
        this.displayName = displayName;
        this.preferredTheme = preferredTheme;
        this.totalGamesPlayed = totalGamesPlayed;
        this.memberSince = memberSince;
    }

    // ---------- getters & setters ----------

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getPreferredTheme() { return preferredTheme; }
    public void setPreferredTheme(String preferredTheme) { this.preferredTheme = preferredTheme; }

    public int getTotalGamesPlayed() { return totalGamesPlayed; }
    public void setTotalGamesPlayed(int totalGamesPlayed) { this.totalGamesPlayed = totalGamesPlayed; }

    public String getMemberSince() { return memberSince; }
    public void setMemberSince(String memberSince) { this.memberSince = memberSince; }
}
