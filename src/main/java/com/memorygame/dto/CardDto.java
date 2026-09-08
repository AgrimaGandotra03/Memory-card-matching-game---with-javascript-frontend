package com.memorygame.dto;

/**
 * Represents a single card in the API response.
 *
 * Privacy rule: symbolKey is set to null for any card that is neither
 * flipped nor matched. The frontend must never infer which cards pair
 * from an unflipped card's symbol — because it won't receive it.
 */
public class CardDto {

    private int cardId;

    /**
     * The card's symbol (emoji or key string).
     * null  → card is face-down (hidden from the player)
     * non-null → card is currently face-up or has been permanently matched
     */
    private String symbolKey;

    private boolean flipped;
    private boolean matched;

    public CardDto() {}

    public CardDto(int cardId, String symbolKey, boolean flipped, boolean matched) {
        this.cardId = cardId;
        this.symbolKey = symbolKey;
        this.flipped = flipped;
        this.matched = matched;
    }

    // ---------- getters & setters ----------

    public int getCardId() { return cardId; }
    public void setCardId(int cardId) { this.cardId = cardId; }

    public String getSymbolKey() { return symbolKey; }
    public void setSymbolKey(String symbolKey) { this.symbolKey = symbolKey; }

    public boolean isFlipped() { return flipped; }
    public void setFlipped(boolean flipped) { this.flipped = flipped; }

    public boolean isMatched() { return matched; }
    public void setMatched(boolean matched) { this.matched = matched; }
}
