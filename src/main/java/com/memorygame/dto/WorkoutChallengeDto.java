package com.memorygame.dto;

import java.util.List;

/**
 * The generated, playable content for one day's workout. Only the fields relevant
 * to that day's skill are populated; the rest are left null so the frontend can
 * tell at a glance which mini-game shape it received.
 */
public class WorkoutChallengeDto {
    private String skill;
    private String level;

    private int previewSeconds;
    private int delaySeconds;
    private int timeLimitSeconds;
    private int gridCells;

    /** Working Memory & Delayed Recall (encoding phase). */
    private List<WorkoutItemDto> items;

    /** Attention & Processing Speed board. */
    private List<WorkoutCardEntry> cards;

    /** Sequence Memory: the fixed set of tiles shown on screen. */
    private List<WorkoutCardEntry> sequenceTiles;

    /** Sequence Memory: the order (by cardId, may repeat) the player must reproduce. */
    private List<Integer> sequence;

    /** Delayed Recall questions, asked after the delay. */
    private List<RecallQuestionDto> questions;

    public String getSkill() { return skill; }
    public void setSkill(String skill) { this.skill = skill; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public int getPreviewSeconds() { return previewSeconds; }
    public void setPreviewSeconds(int previewSeconds) { this.previewSeconds = previewSeconds; }

    public int getDelaySeconds() { return delaySeconds; }
    public void setDelaySeconds(int delaySeconds) { this.delaySeconds = delaySeconds; }

    public int getTimeLimitSeconds() { return timeLimitSeconds; }
    public void setTimeLimitSeconds(int timeLimitSeconds) { this.timeLimitSeconds = timeLimitSeconds; }

    public int getGridCells() { return gridCells; }
    public void setGridCells(int gridCells) { this.gridCells = gridCells; }

    public List<WorkoutItemDto> getItems() { return items; }
    public void setItems(List<WorkoutItemDto> items) { this.items = items; }

    public List<WorkoutCardEntry> getCards() { return cards; }
    public void setCards(List<WorkoutCardEntry> cards) { this.cards = cards; }

    public List<WorkoutCardEntry> getSequenceTiles() { return sequenceTiles; }
    public void setSequenceTiles(List<WorkoutCardEntry> sequenceTiles) { this.sequenceTiles = sequenceTiles; }

    public List<Integer> getSequence() { return sequence; }
    public void setSequence(List<Integer> sequence) { this.sequence = sequence; }

    public List<RecallQuestionDto> getQuestions() { return questions; }
    public void setQuestions(List<RecallQuestionDto> questions) { this.questions = questions; }
}
