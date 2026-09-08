package com.memorygame.controller;

import com.memorygame.dto.ScoreRecordResponse;
import com.memorygame.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for retrieving score history.
 */
@RestController
@RequestMapping("/api/scores")
public class ScoreController {

    private final GameService gameService;

    @Autowired
    public ScoreController(GameService gameService) {
        this.gameService = gameService;
    }

    /**
     * GET /api/scores/{userId}
     * Returns all score history records for the given user, ordered newest first.
     */
    @GetMapping("/{userId}")
    public ResponseEntity<List<ScoreRecordResponse>> getScoreHistory(@PathVariable Long userId) {
        List<ScoreRecordResponse> history = gameService.getScoreHistory(userId);
        return ResponseEntity.ok(history);
    }
}
