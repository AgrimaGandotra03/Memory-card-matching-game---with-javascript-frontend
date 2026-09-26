package com.memorygame.dto;

import java.util.List;

/**
 * A single Delayed Recall question with its multiple-choice options.
 * The correct answer is included because this feature follows the same
 * client-reports-its-own-result trust model already used by the daily
 * challenge and score endpoints in this app — the frontend both asks the
 * question and grades the player's answer, then reports the summary metrics.
 */
public class RecallQuestionDto {
    private String prompt;
    private List<String> options;
    private String correctAnswer;

    public RecallQuestionDto() {}
    public RecallQuestionDto(String prompt, List<String> options, String correctAnswer) {
        this.prompt = prompt;
        this.options = options;
        this.correctAnswer = correctAnswer;
    }

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    public List<String> getOptions() { return options; }
    public void setOptions(List<String> options) { this.options = options; }

    public String getCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }
}
