package com.memorygame.dto;

/**
 * Returned after a successful login or register call.
 * For this prototype we return the userId so the frontend can make
 * subsequent profile/game requests. In a real app this would be a JWT.
 */
public class AuthResponse {

    private Long userId;
    private String username;
    private String displayName;
    private String message;

    // ---------- constructors ----------

    public AuthResponse() {}

    public AuthResponse(Long userId, String username, String displayName, String message) {
        this.userId = userId;
        this.username = username;
        this.displayName = displayName;
        this.message = message;
    }

    // ---------- getters & setters ----------

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
