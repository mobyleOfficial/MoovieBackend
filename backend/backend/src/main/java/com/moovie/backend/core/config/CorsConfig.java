package com.moovie.backend.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Value("${cors.origins:}")
    private String corsOrigins;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                var mapping = registry.addMapping("/**")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");

                String[] origins = buildOrigins();
                if (origins.length > 0) {
                    mapping.allowedOrigins(origins);
                }
            }
        };
    }

    private String[] buildOrigins() {
        java.util.List<String> origins = new java.util.ArrayList<>();
        origins.add("http://localhost:5173");
        origins.add("http://127.0.0.1:5173");

        if (corsOrigins != null && !corsOrigins.isBlank()) {
            for (String origin : corsOrigins.split(",")) {
                String trimmed = origin.trim();
                if (!trimmed.isEmpty()) {
                    origins.add(trimmed);
                }
            }
        }

        return origins.toArray(String[]::new);
    }
}
