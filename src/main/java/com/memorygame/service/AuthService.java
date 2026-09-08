package com.memorygame.service;

import com.memorygame.dto.AuthRequest;
import com.memorygame.dto.AuthResponse;
import com.memorygame.model.PlayerProfile;
import com.memorygame.model.User;
import com.memorygame.repository.PlayerProfileRepository;
import com.memorygame.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles registration and login logic.
 *
 * NOTE: We use BCryptPasswordEncoder ONLY for hashing — we do NOT set up
 * Spring Security filters, sessions, or the full security chain. That's
 * handled by SecurityConfig (which opens all endpoints for local dev).
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PlayerProfileRepository profileRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Autowired
    public AuthService(UserRepository userRepository,
                       PlayerProfileRepository profileRepository,
                       BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registers a new user.
     * Throws IllegalArgumentException if the username is already taken.
     */
    @Transactional
    public AuthResponse register(AuthRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username '" + request.getUsername() + "' is already taken.");
        }

        // Hash the password before saving
        String hashedPassword = passwordEncoder.encode(request.getPassword());
        User newUser = new User(request.getUsername(), hashedPassword);
        newUser = userRepository.save(newUser);

        // Create a blank profile for the new user
        PlayerProfile profile = new PlayerProfile(newUser);
        profileRepository.save(profile);

        return new AuthResponse(
                newUser.getId(),
                newUser.getUsername(),
                profile.getDisplayName(),
                "Registration successful. Welcome, " + newUser.getUsername() + "!"
        );
    }

    /**
     * Logs in an existing user.
     * Throws IllegalArgumentException if credentials are wrong (intentionally
     * vague message to avoid leaking whether the username exists).
     */
    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid username or password.");
        }

        PlayerProfile profile = profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("Profile not found for user " + user.getId()));

        return new AuthResponse(
                user.getId(),
                user.getUsername(),
                profile.getDisplayName(),
                "Login successful. Welcome back, " + user.getUsername() + "!"
        );
    }
}
