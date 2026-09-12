package com.memorygame.service.mode;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.memorygame.model.CardState;
import com.memorygame.model.GameMode;
import com.memorygame.model.GameSession;

import java.time.Instant;
import java.util.List;

public interface GameModeStrategy {

    GameMode mode();

    void initialize(GameSession session, List<CardState> board, Instant startedAt,
                    ObjectMapper objectMapper);

    boolean hasExpired(GameSession session, Instant now);

    void expire(GameSession session);

    boolean isSequenceMode();

    boolean isProgressiveMode();

    default ModeFlipResult flipSequence(GameSession session, List<CardState> board,
                                         int cardId, Instant now,
                                         ObjectMapper objectMapper) {
        throw new IllegalStateException("Sequence actions are not supported by " + mode());
    }
}
