package com.memorygame.controller;

import com.memorygame.dto.*;
import com.memorygame.service.EngagementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/engagement")
public class EngagementController {
    private final EngagementService engagementService;
    public EngagementController(EngagementService engagementService) { this.engagementService = engagementService; }

    @GetMapping("/daily/{userId}")
    public ResponseEntity<DailyChallengeResponse> daily(@PathVariable Long userId,
                                                         @RequestParam(required = false) LocalDate date) {
        return ResponseEntity.ok(engagementService.getDailyChallenge(userId, date));
    }

    @PostMapping("/daily/{userId}/complete")
    public ResponseEntity<DailyChallengeResponse> completeDaily(@PathVariable Long userId,
                                                                 @RequestParam(required = false) LocalDate date,
                                                                 @RequestParam int score) {
        return ResponseEntity.ok(engagementService.completeDailyChallenge(userId, date, score));
    }

    @GetMapping("/badges/{userId}")
    public ResponseEntity<List<BadgeResponse>> badges(@PathVariable Long userId) {
        return ResponseEntity.ok(engagementService.getBadges(userId));
    }

    @GetMapping("/recommendations/{userId}")
    public ResponseEntity<List<RecommendationResponse>> recommendations(@PathVariable Long userId) {
        return ResponseEntity.ok(engagementService.getRecommendations(userId));
    }
}
