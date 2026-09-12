package com.memorygame.service.mode;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.memorygame.model.CardState;
import com.memorygame.model.GameMode;
import com.memorygame.model.GameSession;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class ProgressiveGameModeStrategy implements GameModeStrategy {

    @Override
    public GameMode mode() { return GameMode.PROGRESSIVE; }

    @Override
    public void initialize(GameSession session, List<CardState> board, Instant startedAt,
                            ObjectMapper objectMapper) {}

    @Override
    public boolean hasExpired(GameSession session, Instant now) { return false; }

    @Override
    public void expire(GameSession session) {}

    @Override
    public boolean isSequenceMode() { return false; }

    @Override
    public boolean isProgressiveMode() { return true; }
}
