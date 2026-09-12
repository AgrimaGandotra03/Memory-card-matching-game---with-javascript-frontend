package com.memorygame.controller;

import com.memorygame.dto.FlipCardRequest;
import com.memorygame.dto.GameSessionResponse;
import com.memorygame.dto.HintResponse;
import com.memorygame.dto.StartGameRequest;
import com.memorygame.service.GameService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for all game actions:
 * - Start new game
 * - Flip card
 * - Pause / Resume / Restart
 * - Get hint
 * - Save progress / Load game
 * - Get session details
 */
@RestController
@RequestMapping("/api/game")
public class GameController {

    private final GameService gameService;

    @Autowired
    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    /**
     * POST /api/game/start or POST /api/game/new
     * Body: { "userId": 1, "difficulty": "EASY", "theme": "animals" }
     */
    @PostMapping({"/start", "/new"})
    public ResponseEntity<GameSessionResponse> startNewGame(@Valid @RequestBody StartGameRequest request) {
        GameSessionResponse response = gameService.startNewGame(
                request.getUserId(),
                request.getDifficulty(),
            request.getTheme(),
            request.isFocusMode()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/game/{sessionId}/flip
     * Body: { "cardId": 3 }
     */
    @PostMapping("/{sessionId}/flip")
    public ResponseEntity<GameSessionResponse> flipCard(
            @PathVariable Long sessionId,
            @Valid @RequestBody FlipCardRequest request) {
        GameSessionResponse response = gameService.flipCard(sessionId, request.getCardId());
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/game/{sessionId}/pause
     */
    @PostMapping("/{sessionId}/pause")
    public ResponseEntity<GameSessionResponse> pauseGame(@PathVariable Long sessionId) {
        GameSessionResponse response = gameService.pauseGame(sessionId);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/game/{sessionId}/resume
     */
    @PostMapping("/{sessionId}/resume")
    public ResponseEntity<GameSessionResponse> resumeGame(@PathVariable Long sessionId) {
        GameSessionResponse response = gameService.resumeGame(sessionId);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/game/{sessionId}/restart
     */
    @PostMapping("/{sessionId}/restart")
    public ResponseEntity<GameSessionResponse> restartGame(@PathVariable Long sessionId) {
        GameSessionResponse response = gameService.restartGame(sessionId);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/game/{sessionId}/hint
     */
    @GetMapping("/{sessionId}/hint")
    public ResponseEntity<HintResponse> getHint(@PathVariable Long sessionId) {
        HintResponse response = gameService.getHint(sessionId);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/game/{sessionId}/save
     */
    @PostMapping("/{sessionId}/save")
    public ResponseEntity<GameSessionResponse> saveProgress(@PathVariable Long sessionId) {
        GameSessionResponse response = gameService.saveProgress(sessionId);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/game/load?userId=1
     */
    @GetMapping("/load")
    public ResponseEntity<GameSessionResponse> loadGame(@RequestParam Long userId) {
        GameSessionResponse response = gameService.loadGame(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/game/{sessionId}
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<GameSessionResponse> getSession(@PathVariable Long sessionId) {
        GameSessionResponse response = gameService.getSession(sessionId);
        return ResponseEntity.ok(response);
    }
}
