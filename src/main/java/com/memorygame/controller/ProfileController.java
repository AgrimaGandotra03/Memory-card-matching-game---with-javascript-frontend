package com.memorygame.controller;

import com.memorygame.dto.ErrorResponse;
import com.memorygame.dto.ProfileResponse;
import com.memorygame.dto.ProfileUpdateRequest;
import com.memorygame.model.PlayerProfile;
import com.memorygame.repository.PlayerProfileRepository;
import com.memorygame.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;

/**
 * Handles reading and updating player profiles.
 *
 * GET /api/profile/{userId}  — fetch profile + stats
 * PUT /api/profile/{userId}  — update displayName and/or preferredTheme
 */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final PlayerProfileRepository profileRepository;
    private final UserRepository userRepository;

    // Formatter for the "memberSince" field (e.g. "2024-03-15")
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    public ProfileController(PlayerProfileRepository profileRepository,
                             UserRepository userRepository) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
    }

    /**
     * Fetch a player's profile and stats.
     *
     * Success (200): { "userId": 1, "username": "alice", "displayName": "alice",
     *                  "preferredTheme": null, "totalGamesPlayed": 0, "memberSince": "2024-03-15" }
     * Not found (404): { "error": "No profile found for userId 99" }
     */
    @GetMapping("/{userId}")
    public ResponseEntity<?> getProfile(@PathVariable Long userId) {
        return profileRepository.findByUserId(userId)
                .map(profile -> {
                    ProfileResponse response = toProfileResponse(profile);
                    return ResponseEntity.ok((Object) response);
                })
                .orElseGet(() -> ResponseEntity.status(404)
                        .body(new ErrorResponse("No profile found for userId " + userId)));
    }

    /**
     * Update a player's display name and/or preferred theme.
     * Only the fields you send will be updated (null fields are ignored).
     *
     * Request body: { "displayName": "AliceWonderland", "preferredTheme": "animals" }
     * Success (200): updated ProfileResponse
     * Not found (404): { "error": "..." }
     */
    @PutMapping("/{userId}")
    public ResponseEntity<?> updateProfile(@PathVariable Long userId,
                                           @Valid @RequestBody ProfileUpdateRequest request) {
        return profileRepository.findByUserId(userId)
                .map(profile -> {
                    // Only apply fields that were actually sent in the request
                    if (request.getDisplayName() != null) {
                        profile.setDisplayName(request.getDisplayName());
                    }
                    if (request.getPreferredTheme() != null) {
                        profile.setPreferredTheme(request.getPreferredTheme());
                    }
                    profileRepository.save(profile);
                    return ResponseEntity.ok((Object) toProfileResponse(profile));
                })
                .orElseGet(() -> ResponseEntity.status(404)
                        .body(new ErrorResponse("No profile found for userId " + userId)));
    }

    // ---------- helpers ----------

    private ProfileResponse toProfileResponse(PlayerProfile profile) {
        return new ProfileResponse(
                profile.getUser().getId(),
                profile.getUser().getUsername(),
                profile.getDisplayName(),
                profile.getPreferredTheme(),
                profile.getTotalGamesPlayed(),
                profile.getCreatedAt().format(DATE_FMT)
        );
    }
}
