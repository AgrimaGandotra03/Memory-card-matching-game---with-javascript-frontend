package com.memorygame.service.mode;

import java.util.List;

public class ModeFlipResult {

    private final String flipResult;
    private final boolean wonGame;
    private final boolean lostGame;
    private final List<Integer> revealedThisMove;

    public ModeFlipResult(String flipResult, boolean wonGame, boolean lostGame,
                          List<Integer> revealedThisMove) {
        this.flipResult = flipResult;
        this.wonGame = wonGame;
        this.lostGame = lostGame;
        this.revealedThisMove = revealedThisMove;
    }

    public String getFlipResult() { return flipResult; }
    public boolean isWonGame() { return wonGame; }
    public boolean isLostGame() { return lostGame; }
    public List<Integer> getRevealedThisMove() { return revealedThisMove; }
}
