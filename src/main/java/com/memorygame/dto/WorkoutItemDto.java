package com.memorygame.dto;

/** A single "remember this symbol at this position" item, used by Working Memory and Delayed Recall. */
public class WorkoutItemDto {
    private int position;
    private String symbol;

    public WorkoutItemDto() {}
    public WorkoutItemDto(int position, String symbol) {
        this.position = position;
        this.symbol = symbol;
    }

    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
}
