package com.archiveat.server.global.config;

import com.archiveat.server.global.jwt.JwtAuthenticationEntryPoint;
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

import org.springframework.http.HttpMethod;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtUtil jwtUtil;
        private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                return http
                                .csrf(csrf -> csrf.disable()) // REST API면 보통 disable
                                .cors(Customizer.withDefaults())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .formLogin(form -> form.disable()) // 기본 /login 페이지 끄기
                                .httpBasic(basic -> basic.disable()) // Basic 인증 요구 끄기
                                .exceptionHandling(exception -> exception
                                                .authenticationEntryPoint(jwtAuthenticationEntryPoint))
                                .authorizeHttpRequests(auth -> auth
                                                // 공개 API
                                        // ✅ 헬스체크는 인증 없이 허용
                                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()

                                                .requestMatchers("/auth/**").permitAll()
                                                .requestMatchers("/archiveat-docs/**", "/v3/api-docs/**",
                                                                "/swagger-ui/**", "/swagger-resources/**")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.GET, "/user/metadata").permitAll()
                                                // 나머지 전부 인증 필수
                                                .anyRequest().authenticated())
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
