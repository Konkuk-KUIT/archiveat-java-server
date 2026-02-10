package com.archiveat.server.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                        "http://localhost:3000", // 로컬에서 개발 중인 프론트엔드 (React/Vue 등)
                        "http://localhost:8080", // 로컬 백엔드/Swagger
                        "http://archiveat.io.kr", // 배포된 프론트엔드 (HTTP)
                        "https://archiveat.io.kr" // 배포된 프론트엔드 (HTTPS)
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}