package com.memorygame.dto;

import java.time.LocalDate;
import java.util.List;

public class DailyChallengeResponse {
    private LocalDate challengeDate;
    private long seed;
    private boolean completed;
    private Integer completionScore;
    private List<CardDto> board;
    public LocalDate getChallengeDate() { return challengeDate; }
    public void setChallengeDate(LocalDate challengeDate) { this.challengeDate = challengeDate; }
    public long getSeed() { return seed; }
    public void setSeed(long seed) { this.seed = seed; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public Integer getCompletionScore() { return completionScore; }
    public void setCompletionScore(Integer completionScore) { this.completionScore = completionScore; }
    public List<CardDto> getBoard() { return board; }
    public void setBoard(List<CardDto> board) { this.board = board; }
}
