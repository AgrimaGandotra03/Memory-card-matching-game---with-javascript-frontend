package com.memorygame.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request body for POST /api/game/{sessionId}/flip
 *
 * Example:
 *   { "cardId": 5 }
 */
public class FlipCardRequest {

    @NotNull(message = "cardId is required")
    private Integer cardId;

    // ---------- getters & setters ----------

    public Integer getCardId() { return cardId; }
    public void setCardId(Integer cardId) { this.cardId = cardId; }
}
