package com.archiveat.server.global.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. 요청 헤더에서 "Bearer "로 시작하는 토큰을 추출합니다.
        String authorizationHeader = request.getHeader("Authorization");
        String token = jwtUtil.resolveToken(authorizationHeader);

        // 2. 토큰이 존재하고 유효한지 검증합니다.
        if (token != null) {
            try {
                jwtUtil.validate(token);

                // 3. 토큰에서 유저 식별자(userId)를 꺼냅니다.
                String userIdString = String.valueOf(jwtUtil.getUserId(token));
                Long userId = Long.parseLong(userIdString);

                // 4. Spring Security가 인식할 수 있는 인증 객체를 생성합니다.
                // 현재는 권한 정보가 없으므로 빈 리스트를 전달합니다.
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());

                // 5. 서버 내부의 보안 저장소(SecurityContext)에 이 유저가 인증되었음을 기록합니다.
                // 이후 컨트롤러에서 이 정보를 꺼내 쓸 수 있게 됩니다.
                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (Exception e) {
                // 토큰이 만료되었거나 변조된 경우 context에 인증 정보를 등록하지 않습니다.
                // 이 경우 이후 SecurityConfig 설정에 따라 접근이 거부됩니다.
            }
        }

        // 6. 다음 필터로 요청을 넘깁니다.
        filterChain.doFilter(request, response);
    }
}