package com.memorygame.dto;

import java.util.List;

/**
 * Response for GET /api/game/{sessionId}/hint
 *
 * Example:
 *   {
 *     "hintCardIds": [3, 11],
 *     "hintsUsed": 2,
 *     "scorePenaltyApplied": 50,
 *     "message": "Two matching cards revealed. Penalty of 50 points applied."
 *   }
 */
public class HintResponse {

    /** The two card IDs that form the first unmatched pair found. */
    private List<Integer> hintCardIds;

    /** Total hints used in this session after this request. */
    private int hintsUsed;

    /** The score deduction that will be applied at win time. */
    private int scorePenaltyApplied;

    private String message;

    public HintResponse() {}

    public HintResponse(List<Integer> hintCardIds, int hintsUsed, int scorePenaltyApplied) {
        this.hintCardIds = hintCardIds;
        this.hintsUsed = hintsUsed;
        this.scorePenaltyApplied = scorePenaltyApplied;
        this.message = "Two matching cards revealed. Penalty of " + scorePenaltyApplied + " points applied.";
    }

    // ---------- getters & setters ----------

    public List<Integer> getHintCardIds() { return hintCardIds; }
    public void setHintCardIds(List<Integer> hintCardIds) { this.hintCardIds = hintCardIds; }

    public int getHintsUsed() { return hintsUsed; }
    public void setHintsUsed(int hintsUsed) { this.hintsUsed = hintsUsed; }

    public int getScorePenaltyApplied() { return scorePenaltyApplied; }
    public void setScorePenaltyApplied(int scorePenaltyApplied) { this.scorePenaltyApplied = scorePenaltyApplied; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
