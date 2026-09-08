package com.memorygame.dto;

import jakarta.validation.constraints.Size;

/**
 * Request body for PUT /api/profile/{userId}.
 * All fields are optional — only non-null fields will update the profile.
 */
public class ProfileUpdateRequest {

    @Size(max = 50, message = "Display name can be at most 50 characters")
    private String displayName;

    @Size(max = 30, message = "Theme name can be at most 30 characters")
    private String preferredTheme;

    // ---------- getters & setters ----------

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getPreferredTheme() { return preferredTheme; }
    public void setPreferredTheme(String preferredTheme) { this.preferredTheme = preferredTheme; }
}
