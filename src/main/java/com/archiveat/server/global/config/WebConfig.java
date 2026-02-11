package com.archiveat.server.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // 이전 설정(allowedOrigins) - 변경되어 주석 처리
                // .allowedOrigins(
                //         "http://localhost:3000", // 로컬에서 개발 중인 프론트엔드 (React/Vue 등)
                //         "http://localhost:8080", // 로컬 백엔드/Swagger
                //         "http://archiveat.io.kr", // 배포된 프론트엔드 (HTTP)
                //         "https://archiveat.io.kr" // 배포된 프론트엔드 (HTTPS)
                // )
                // 이전 설정(allowedOriginPatterns 전체) - 변경되어 주석 처리
                // .allowedOriginPatterns(
                //         "http://localhost:*",
                //         "https://localhost:*",
                //         "http://127.0.0.1:*",
                //         "https://127.0.0.1:*",
                //         "http://*.archiveat.io.kr",
                //         "https://*.archiveat.io.kr",
                //         "http://archiveat.io.kr",
                //         "https://archiveat.io.kr",
                //         "https://api.archiveat.io.kr"
                // )
                .allowedOriginPatterns(
                        "http://localhost:*",
                        "http://127.0.0.1:*",
                        "https://archiveat.io.kr"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization", "Set-Cookie")
                .allowCredentials(true);
    }
}
