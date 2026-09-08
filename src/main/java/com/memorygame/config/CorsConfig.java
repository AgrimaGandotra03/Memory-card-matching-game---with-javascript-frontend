package com.memorygame.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Allows the browser frontend (running on any localhost port or any origin
 * during development) to call our API without being blocked by CORS.
 *
 * When you deploy, replace allowedOrigins("*") with the exact domain(s)
 * of your frontend.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")      // only our API paths need CORS headers
                .allowedOriginPatterns("*") // any origin (localhost:5500, localhost:3000, etc.)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false)    // set to true only if you add cookies/sessions later
                .maxAge(3600);             // browsers cache the preflight for 1 hour
    }
}
