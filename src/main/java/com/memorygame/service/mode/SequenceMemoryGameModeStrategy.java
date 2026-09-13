package com.memorygame.service.mode;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.memorygame.model.CardState;
import com.memorygame.model.GameMode;
import com.memorygame.model.GameSession;
import com.memorygame.model.GameStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class SequenceMemoryGameModeStrategy implements GameModeStrategy {

    private static final int STARTING_SEQUENCE_LENGTH = 5;
    private static final int MAX_SEQUENCE_LENGTH = 12;

    @Override
    public GameMode mode() { return GameMode.SEQUENCE_MEMORY; }

    @Override
    public void initialize(GameSession session, List<CardState> board, Instant startedAt,
                            ObjectMapper objectMapper) {
        List<Integer> order = new ArrayList<>();
        for (CardState card : board) order.add(card.getCardId());
        Collections.shuffle(order);
        int sequenceLength = Math.min(
                Math.min(STARTING_SEQUENCE_LENGTH + session.getLevel() - 1, MAX_SEQUENCE_LENGTH),
                order.size());
        order = new ArrayList<>(order.subList(0, sequenceLength));
        session.setSequenceOrderJson(toJson(order, objectMapper));
        session.setSequencePosition(0);
        session.setSequencePlaybackStartedAt(startedAt);
        session.setSequencePlaybackEndsAt(startedAt.plusSeconds(sequenceLength));
    }

    @Override
    public boolean hasExpired(GameSession session, Instant now) { return false; }

    @Override
    public void expire(GameSession session) {}

    @Override
    public boolean isSequenceMode() { return true; }

    @Override
    public boolean isProgressiveMode() { return false; }

    @Override
    public ModeFlipResult flipSequence(GameSession session, List<CardState> board,
                                       int cardId, Instant now,
                                       ObjectMapper objectMapper) {
        if (session.getSequencePlaybackEndsAt() != null
                && now.isBefore(session.getSequencePlaybackEndsAt())) {
            throw new IllegalStateException("Sequence playback is still active. Watch the order first.");
        }

        List<Integer> order = sequenceOrder(session, objectMapper);
        int position = session.getSequencePosition();
        if (position >= order.size()) {
            throw new IllegalStateException("Sequence is already complete.");
        }

        CardState card = board.stream()
                .filter(candidate -> candidate.getCardId() == cardId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Card ID " + cardId + " does not exist on this board."));
        if (card.isMatched()) {
            throw new IllegalArgumentException("Card " + cardId + " was already selected.");
        }

        session.setMoves(session.getMoves() + 1);
        if (order.get(position) != cardId) {
            session.setMistakeCount(session.getMistakeCount() + 1);
            session.setCurrentConsecutiveMatchStreak(0);
            session.setAccuracyPercent((position * 100.0) / order.size());
            session.setStatus(GameStatus.LOST);
            card.setFlipped(true);
            return new ModeFlipResult("SEQUENCE_MISMATCH", false, true, List.of(cardId));
        }

        card.setFlipped(true);
        card.setMatched(true);
        session.setSequencePosition(position + 1);
        session.setCurrentConsecutiveMatchStreak(session.getCurrentConsecutiveMatchStreak() + 1);
        session.setMaxConsecutiveMatchStreak(Math.max(
                session.getMaxConsecutiveMatchStreak(), session.getCurrentConsecutiveMatchStreak()));
        session.setAccuracyPercent((session.getSequencePosition() * 100.0) / order.size());

        boolean won = session.getSequencePosition() == order.size();
        if (won) session.setStatus(GameStatus.WON);
        return new ModeFlipResult("SEQUENCE_CORRECT", won, false, List.of(cardId));
    }

    public List<Integer> sequenceOrder(GameSession session, ObjectMapper objectMapper) {
        try {
            return objectMapper.readValue(session.getSequenceOrderJson(), new TypeReference<List<Integer>>() {});
        } catch (Exception e) {
            throw new IllegalStateException("Sequence state is corrupted.", e);
        }
    }

    private String toJson(List<Integer> order, ObjectMapper objectMapper) {
        try {
            return objectMapper.writeValueAsString(order);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to store sequence state.", e);
        }
    }
}
