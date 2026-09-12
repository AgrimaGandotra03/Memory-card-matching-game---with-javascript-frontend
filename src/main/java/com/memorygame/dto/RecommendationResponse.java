package com.memorygame.dto;

public class RecommendationResponse {
    private String title;
    private String message;
    private String mode;
    public RecommendationResponse() {}
    public RecommendationResponse(String title, String message, String mode) {
        this.title = title; this.message = message; this.mode = mode;
    }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getMode() { return mode; }
}
