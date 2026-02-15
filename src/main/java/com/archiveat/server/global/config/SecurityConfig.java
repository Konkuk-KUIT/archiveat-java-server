package com.archiveat.server.global.config;

import com.archiveat.server.global.jwt.JwtAuthenticationFilter;
import com.archiveat.server.global.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtUtil jwtUtil;

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                return http
                                .csrf(csrf -> csrf.disable()) // REST API면 보통 disable
                                // 이전: SecurityFilterChain에 CORS 설정 없음
                                // .cors(...) 없음
                                .cors(Customizer.withDefaults())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .formLogin(form -> form.disable()) // 기본 /login 페이지 끄기
                                .httpBasic(basic -> basic.disable()) // Basic 인증 요구 끄기
                                .authorizeHttpRequests(auth -> auth

                                                .requestMatchers("/user/**").authenticated() // 온보딩 관련 /user 경로는 인증 필요
                                                .anyRequest().permitAll() // 일단 전체 오픈(개발용)
                                )
                                .addFilterBefore(new JwtAuthenticationFilter(jwtUtil),
                                                UsernamePasswordAuthenticationFilter.class)
                                .build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                // 이전 설정(allowedOriginPatterns 전체) - 변경되어 주석 처리
                // configuration.setAllowedOriginPatterns(List.of(
                // "http://localhost:*",
                // "https://localhost:*",
                // "http://127.0.0.1:*",
                // "https://127.0.0.1:*",
                // "http://*.archiveat.io.kr",
                // "https://*.archiveat.io.kr",
                // "http://archiveat.io.kr",
                // "https://archiveat.io.kr",
                // "https://api.archiveat.io.kr"
                // ));
                configuration.setAllowedOriginPatterns(List.of(
                                "http://localhost:*",
                                "http://127.0.0.1:*",
                                "https://archiveat.io.kr"));
                configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
                configuration.setAllowedHeaders(List.of("*"));
                configuration.setExposedHeaders(List.of("Authorization", "Set-Cookie"));
                configuration.setAllowCredentials(true);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }
}
