package com.memorygame.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Provides the BCryptPasswordEncoder bean used in AuthService.
 * We also disable Spring Security's auto-configured HTTP filters here
 * so that all endpoints are accessible without any login/session.
 * (For a production app you'd set up proper authentication instead.)
 */
@Configuration
public class SecurityConfig {

    /**
     * BCryptPasswordEncoder is used ONLY for hashing/verifying passwords.
     * Spring Security's HTTP security filters are turned off via
     * the SecurityFilterChain bean below.
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Permit all requests so that our simple prototype is accessible
     * from curl / Postman / a plain HTML frontend without any auth headers.
     *
     * The H2 console also needs frameOptions disabled because it renders
     * inside an iframe.
     */
    @Bean
    public org.springframework.security.web.SecurityFilterChain securityFilterChain(
            org.springframework.security.config.annotation.web.builders.HttpSecurity http) throws Exception {

        http
            // Disable CSRF protection — not needed for a stateless REST API
            .csrf(csrf -> csrf.disable())
            // Allow the H2 console iframe to load
            .headers(headers -> headers.frameOptions(
                    org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig::disable))
            // Allow every request without authentication
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }
}
