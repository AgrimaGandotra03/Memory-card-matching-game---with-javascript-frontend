package com.memorygame.dto;

/**
 * Result metrics reported by the frontend after a Daily Brain Workout attempt.
 * Not every field applies to every skill — the service reads only what's relevant.
 */
public class WorkoutSubmitRequest {
    private String skill;
    private String level;
    private int correctCount;
    private int incorrectCount;
    private int totalItems;
    private int timeTakenSeconds;

    private Long avgReactionTimeMillis;
    private Integer matchesFound;
    private Integer distractionsIgnored;
    private Integer sequenceLength;
    private Integer retries;
    private Double immediateAccuracyPercent;
    private Double delayedAccuracyPercent;

    public String getSkill() { return skill; }
    public void setSkill(String skill) { this.skill = skill; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public int getCorrectCount() { return correctCount; }
    public void setCorrectCount(int correctCount) { this.correctCount = correctCount; }

    public int getIncorrectCount() { return incorrectCount; }
    public void setIncorrectCount(int incorrectCount) { this.incorrectCount = incorrectCount; }

    public int getTotalItems() { return totalItems; }
    public void setTotalItems(int totalItems) { this.totalItems = totalItems; }

    public int getTimeTakenSeconds() { return timeTakenSeconds; }
    public void setTimeTakenSeconds(int timeTakenSeconds) { this.timeTakenSeconds = timeTakenSeconds; }

    public Long getAvgReactionTimeMillis() { return avgReactionTimeMillis; }
    public void setAvgReactionTimeMillis(Long avgReactionTimeMillis) { this.avgReactionTimeMillis = avgReactionTimeMillis; }

    public Integer getMatchesFound() { return matchesFound; }
    public void setMatchesFound(Integer matchesFound) { this.matchesFound = matchesFound; }

    public Integer getDistractionsIgnored() { return distractionsIgnored; }
    public void setDistractionsIgnored(Integer distractionsIgnored) { this.distractionsIgnored = distractionsIgnored; }

    public Integer getSequenceLength() { return sequenceLength; }
    public void setSequenceLength(Integer sequenceLength) { this.sequenceLength = sequenceLength; }

    public Integer getRetries() { return retries; }
    public void setRetries(Integer retries) { this.retries = retries; }

    public Double getImmediateAccuracyPercent() { return immediateAccuracyPercent; }
    public void setImmediateAccuracyPercent(Double immediateAccuracyPercent) { this.immediateAccuracyPercent = immediateAccuracyPercent; }

    public Double getDelayedAccuracyPercent() { return delayedAccuracyPercent; }
    public void setDelayedAccuracyPercent(Double delayedAccuracyPercent) { this.delayedAccuracyPercent = delayedAccuracyPercent; }
}
