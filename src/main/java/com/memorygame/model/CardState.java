package com.memorygame.model;

/**
 * Represents one card slot on the board.
 *
 * This is NOT a JPA entity — the full list is stored as a single JSON string
 * in GameSession.boardStateJson and serialized/deserialized with Jackson.
 *
 * Field naming note: we avoid the 'is' prefix on booleans to prevent
 * Jackson serialization oddities (e.g. "flipped" instead of "isFlipped").
 */
public class CardState {

    private int cardId;
    private String symbolKey; // e.g. "🐶" — the unique identifier for matching pairs
    private boolean flipped;  // true while face-up (temporarily, during a turn)
    private boolean matched;  // true permanently once the pair is found

    public CardState() {}

    public CardState(int cardId, String symbolKey) {
        this.cardId = cardId;
        this.symbolKey = symbolKey;
        this.flipped = false;
        this.matched = false;
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
