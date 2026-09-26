package com.memorygame.controller;

import com.memorygame.dto.DailyWorkoutResponse;
import com.memorygame.dto.WorkoutAttemptResponse;
import com.memorygame.dto.WorkoutProgressResponse;
import com.memorygame.dto.WorkoutSubmitRequest;
import com.memorygame.service.DailyWorkoutService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/workouts")
public class DailyWorkoutController {

    private final DailyWorkoutService dailyWorkoutService;

    public DailyWorkoutController(DailyWorkoutService dailyWorkoutService) {
        this.dailyWorkoutService = dailyWorkoutService;
    }

    @GetMapping("/{userId}/today")
    public ResponseEntity<DailyWorkoutResponse> today(@PathVariable Long userId,
                                                        @RequestParam(required = false) LocalDate date) {
        return ResponseEntity.ok(dailyWorkoutService.getTodayWorkout(userId, date));
    }

    @PostMapping("/{userId}/submit")
    public ResponseEntity<WorkoutAttemptResponse> submit(@PathVariable Long userId,
                                                          @RequestBody WorkoutSubmitRequest request) {
        return ResponseEntity.ok(dailyWorkoutService.submitAttempt(userId, request));
    }

    @GetMapping("/{userId}/progress")
    public ResponseEntity<WorkoutProgressResponse> progress(@PathVariable Long userId) {
        return ResponseEntity.ok(dailyWorkoutService.getProgress(userId));
    }
}
