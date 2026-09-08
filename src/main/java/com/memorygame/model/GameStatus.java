package com.memorygame.model;

/**
 * Lifecycle states for a GameSession.
 */
public enum GameStatus {
    ACTIVE,   // timer running, player can flip cards
    PAUSED,   // timer frozen, board visible but cards unflippable
    WON,      // all pairs matched — session complete
    LOST      // reserved for future timed-out / gave-up flow
}
