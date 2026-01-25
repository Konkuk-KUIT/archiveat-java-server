package com.archiveat.server.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())          // REST API면 보통 disable
                .formLogin(form -> form.disable())     // 기본 /login 페이지 끄기
                .httpBasic(basic -> basic.disable())   // Basic 인증 요구 끄기
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll() // signup/login/emailcheck 열기
                        .anyRequest().permitAll()               // 일단 전체 오픈(개발용)
                )
                .build();
    }
}
