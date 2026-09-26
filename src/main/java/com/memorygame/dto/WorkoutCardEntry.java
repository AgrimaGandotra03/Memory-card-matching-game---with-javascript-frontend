package com.memorygame.dto;

/**
 * A single card in an Attention / Processing-Speed / Sequence-Memory board.
 * "decoy" marks a card with no matching partner on the board (Attention mode only) —
 * clicking it should count as an ignored-distraction or a mistake, depending on the mini-game.
 */
public class WorkoutCardEntry {
    private int cardId;
    private String symbol;
    private boolean decoy;

    public WorkoutCardEntry() {}
    public WorkoutCardEntry(int cardId, String symbol, boolean decoy) {
        this.cardId = cardId;
        this.symbol = symbol;
        this.decoy = decoy;
    }

    public int getCardId() { return cardId; }
    public void setCardId(int cardId) { this.cardId = cardId; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public boolean isDecoy() { return decoy; }
    public void setDecoy(boolean decoy) { this.decoy = decoy; }
}
