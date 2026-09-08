package com.memorygame;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.memorygame.dto.GameSessionResponse;
import com.memorygame.dto.HintResponse;
import com.memorygame.dto.ScoreRecordResponse;
import com.memorygame.model.*;
import com.memorygame.repository.GameSessionRepository;
import com.memorygame.repository.PlayerProfileRepository;
import com.memorygame.repository.ScoreRecordRepository;
import com.memorygame.repository.UserRepository;
import com.memorygame.service.GameService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class GameServiceTest {

    @Autowired
    private GameService gameService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameSessionRepository sessionRepository;

    @Autowired
    private ScoreRecordRepository scoreRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("testuser_" + System.currentTimeMillis());
        testUser.setPasswordHash("hashed_pw");
        testUser = userRepository.save(testUser);
    }

    @Test
    void testStartNewGame_EasyDifficulty() {
        GameSessionResponse session = gameService.startNewGame(testUser.getId(), Difficulty.EASY, "animals");

        assertNotNull(session);
        assertNotNull(session.getSessionId());
        assertEquals("EASY", session.getDifficulty());
        assertEquals("animals", session.getTheme());
        assertEquals(0, session.getMoves());
        assertEquals("ACTIVE", session.getStatus());
        assertEquals(16, session.getBoard().size()); // 4x4

        // All symbols should be masked initially
        session.getBoard().forEach(card -> {
            assertNull(card.getSymbolKey(), "Unflipped card symbol should be hidden");
            assertFalse(card.isFlipped());
            assertFalse(card.isMatched());
        });
    }

    @Test
    void testStartNewGame_MediumAndHardDifficulty() {
        GameSessionResponse mediumSession = gameService.startNewGame(testUser.getId(), Difficulty.MEDIUM, "fruits");
        assertEquals(36, mediumSession.getBoard().size()); // 6x6

        GameSessionResponse hardSession = gameService.startNewGame(testUser.getId(), Difficulty.HARD, "space");
        assertEquals(64, hardSession.getBoard().size()); // 8x8
    }

    @Test
    void testFlipCard_FirstAndSecondFlip() {
        GameSessionResponse session = gameService.startNewGame(testUser.getId(), Difficulty.EASY, "animals");
        Long sessionId = session.getSessionId();

        // 1st flip
        GameSessionResponse flip1 = gameService.flipCard(sessionId, 0);
        assertEquals("FIRST_FLIP", flip1.getFlipResult());
        assertTrue(flip1.getBoard().get(0).isFlipped());
        assertNotNull(flip1.getBoard().get(0).getSymbolKey());
        assertEquals(0, flip1.getMoves());

        // 2nd flip
        GameSessionResponse flip2 = gameService.flipCard(sessionId, 1);
        assertNotNull(flip2.getFlipResult());
        assertTrue(flip2.getFlipResult().equals("MATCH") || flip2.getFlipResult().equals("NO_MATCH"));
        assertEquals(1, flip2.getMoves());
    }

    @Test
    void testPauseAndResumeGame() {
        GameSessionResponse session = gameService.startNewGame(testUser.getId(), Difficulty.EASY, "animals");
        Long sessionId = session.getSessionId();

        GameSessionResponse paused = gameService.pauseGame(sessionId);
        assertEquals("PAUSED", paused.getStatus());

        GameSessionResponse resumed = gameService.resumeGame(sessionId);
        assertEquals("ACTIVE", resumed.getStatus());
    }

    @Test
    void testGetHint() {
        GameSessionResponse session = gameService.startNewGame(testUser.getId(), Difficulty.EASY, "animals");
        Long sessionId = session.getSessionId();

        HintResponse hint = gameService.getHint(sessionId);
        assertNotNull(hint);
        assertEquals(2, hint.getHintCardIds().size());
        assertEquals(1, hint.getHintsUsed());
        assertEquals(50, hint.getScorePenaltyApplied());
    }

    @Test
    void testRestartGame() {
        GameSessionResponse session = gameService.startNewGame(testUser.getId(), Difficulty.EASY, "animals");
        Long sessionId = session.getSessionId();

        gameService.flipCard(sessionId, 0);
        gameService.flipCard(sessionId, 1);

        GameSessionResponse restarted = gameService.restartGame(sessionId);
        assertEquals(0, restarted.getMoves());
        assertEquals(0, restarted.getHintsUsed());
        assertEquals("ACTIVE", restarted.getStatus());
        assertEquals(16, restarted.getBoard().size());
    }

    @Test
    void testCompleteGameAndScoreRecord() {
        GameSessionResponse session = gameService.startNewGame(testUser.getId(), Difficulty.EASY, "animals");
        Long sessionId = session.getSessionId();

        // Read internal session to get board symbols and solve the game
        GameSession internalSession = sessionRepository.findById(sessionId).orElseThrow();
        ObjectMapper mapper = new ObjectMapper();
        try {
            List<CardState> cards = mapper.readValue(
                    internalSession.getBoardStateJson(),
                    new com.fasterxml.jackson.core.type.TypeReference<List<CardState>>() {}
            );

            // Group card IDs by symbol
            var grouped = cards.stream().collect(
                    java.util.stream.Collectors.groupingBy(CardState::getSymbolKey)
            );

            GameSessionResponse lastResponse = null;
            for (var entry : grouped.values()) {
                int id1 = entry.get(0).getCardId();
                int id2 = entry.get(1).getCardId();
                gameService.flipCard(sessionId, id1);
                lastResponse = gameService.flipCard(sessionId, id2);
            }

            assertNotNull(lastResponse);
            assertTrue(lastResponse.isWonGame());
            assertEquals("WON", lastResponse.getStatus());
            assertTrue(lastResponse.getScore() > 0);

            // Verify score history recorded
            List<ScoreRecordResponse> history = gameService.getScoreHistory(testUser.getId());
            assertFalse(history.isEmpty());
            assertEquals(testUser.getId(), history.get(0).getUserId());
            assertEquals("EASY", history.get(0).getDifficulty());
            assertEquals(8, history.get(0).getMoves());
        } catch (Exception e) {
            fail("Exception during board solving: " + e.getMessage());
        }
    }
}
