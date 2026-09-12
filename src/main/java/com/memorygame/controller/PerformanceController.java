package com.memorygame.controller;

import com.memorygame.dto.PerformanceHistoryResponse;
import com.memorygame.service.GameService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@RestController
@RequestMapping("/api/performance")
public class PerformanceController {

    private final GameService gameService;

    @Autowired
    public PerformanceController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<PerformanceHistoryResponse>> getHistory(@PathVariable Long userId) {
        return ResponseEntity.ok(gameService.getPerformanceHistory(userId));
    }
}