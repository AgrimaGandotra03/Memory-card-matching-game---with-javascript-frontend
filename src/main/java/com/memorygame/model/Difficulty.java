package com.memorygame.model;

/**
 * Maps to board grid sizes:
 *   EASY   → 4×4  = 8 pairs  (16 cards)
 *   MEDIUM → 6×6  = 18 pairs (36 cards)
 *   HARD   → 8×8  = 32 pairs (64 cards)
 */
public enum Difficulty {
    EASY,
    MEDIUM,
    HARD
}
