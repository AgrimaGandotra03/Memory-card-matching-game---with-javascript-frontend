package com.memorygame.service.mode;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.memorygame.model.CardState;
import com.memorygame.model.Difficulty;
import com.memorygame.model.GameMode;
import com.memorygame.model.GameSession;
import com.memorygame.model.GameStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class TimedChallengeGameModeStrategy implements GameModeStrategy {

    private static final Map<Difficulty, Integer> TIME_LIMIT_SECONDS = Map.of(
            Difficulty.EASY, 60,
            Difficulty.MEDIUM, 90,
            Difficulty.HARD, 120
    );

    @Override
    public GameMode mode() { return GameMode.TIMED_CHALLENGE; }

    @Override
    public void initialize(GameSession session, List<CardState> board, Instant startedAt,
                            ObjectMapper objectMapper) {
        int limit = TIME_LIMIT_SECONDS.getOrDefault(session.getDifficulty(), 60);
        session.setTotalTimeLimitSeconds(limit);
        session.setGameDeadlineAt(startedAt.plusSeconds(limit));
    }

    @Override
    public boolean hasExpired(GameSession session, Instant now) {
        return session.getGameDeadlineAt() != null
                && !now.isBefore(session.getGameDeadlineAt())
                && session.getStatus() == GameStatus.ACTIVE;
    }

    @Override
    public void expire(GameSession session) {
        session.setStatus(GameStatus.LOST);
    }

    @Override
    public boolean isSequenceMode() { return false; }

    @Override
    public boolean isProgressiveMode() { return false; }
}
