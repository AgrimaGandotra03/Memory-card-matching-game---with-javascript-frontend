package com.memorygame.dto;

import java.time.LocalDateTime;

public class BadgeResponse {
    private String code;
    private String name;
    private String description;
    private LocalDateTime earnedAt;
    public BadgeResponse() {}
    public BadgeResponse(String code, String name, String description, LocalDateTime earnedAt) {
        this.code = code; this.name = name; this.description = description; this.earnedAt = earnedAt;
    }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public LocalDateTime getEarnedAt() { return earnedAt; }
}
