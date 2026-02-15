package com.archiveat.server.global.config;

import com.archiveat.server.domain.explore.controller.ExploreController;
import com.archiveat.server.domain.explore.service.ExploreService;
import com.archiveat.server.domain.home.controller.HomeController;
import com.archiveat.server.domain.home.service.HomeService;
import com.archiveat.server.domain.user.controller.OnboardingController;
import com.archiveat.server.domain.user.service.OnboardingService;
import com.archiveat.server.global.jwt.JwtAuthenticationEntryPoint;
import com.archiveat.server.global.jwt.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Spring Security 인증 정책 테스트.
 *
 * 검증 항목:
 * - 보호 API: 토큰 없으면 401 + JSON 에러 응답
 * - 보호 API: 유효 토큰이면 200 (인증 통과)
 * - 보호 API: 만료/변조 토큰이면 401
 * - 공개 API: 토큰 없이 접근 가능
 */
@WebMvcTest({ HomeController.class, ExploreController.class, OnboardingController.class })
@Import({ JwtUtil.class, SecurityConfig.class, JwtAuthenticationEntryPoint.class })
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    // 컨트롤러가 의존하는 서비스 Mock
    @MockitoBean
    private HomeService homeService;
    @MockitoBean
    private ExploreService exploreService;
    @MockitoBean
    private OnboardingService onboardingService;

    // ── 보호 API: 토큰 없음 → 401 ──────────────────────

    @Nested
    @DisplayName("보호 API - 토큰 없음 → 401 Unauthorized")
    class ProtectedEndpointsWithoutToken {

        @Test
        @DisplayName("GET /home → 401 + JSON 에러 응답")
        void home_withoutToken_returns401() throws Exception {
            mockMvc.perform(get("/home"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentTypeCompatibleWith("application/json"))
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.statusCode").value(40100))
                    .andExpect(jsonPath("$.message").value("인증에 실패했습니다."));
        }

        @Test
        @DisplayName("GET /explore → 401")
        void explore_withoutToken_returns401() throws Exception {
            mockMvc.perform(get("/explore"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.statusCode").value(40100));
        }

        @Test
        @DisplayName("GET /user/nickname → 401 (기존 403에서 변경)")
        void userNickname_withoutToken_returns401() throws Exception {
            mockMvc.perform(get("/user/nickname"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.statusCode").value(40100));
        }
    }

    // ── 보호 API: 유효 토큰 → 200 (인증 통과) ──────────

    @Nested
    @DisplayName("보호 API - 유효 토큰 → 인증 통과")
    class ProtectedEndpointsWithValidToken {

        @Test
        @DisplayName("GET /home + Bearer token → 200")
        void home_withValidToken_returns200() throws Exception {
            String token = jwtUtil.generateAccessToken(1L);

            mockMvc.perform(get("/home")
                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /explore + Bearer token → 200")
        void explore_withValidToken_returns200() throws Exception {
            String token = jwtUtil.generateAccessToken(1L);

            mockMvc.perform(get("/explore")
                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
    }

    // ── 보호 API: 만료/변조 토큰 → 401 ─────────────────

    @Nested
    @DisplayName("보호 API - 잘못된 토큰 → 401")
    class ProtectedEndpointsWithInvalidToken {

        @Test
        @DisplayName("GET /home + 변조된 토큰 → 401")
        void home_withTamperedToken_returns401() throws Exception {
            mockMvc.perform(get("/home")
                    .header("Authorization", "Bearer invalid.token.here"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.statusCode").value(40100));
        }

        @Test
        @DisplayName("GET /explore + 만료된 토큰 형식 → 401")
        void explore_withExpiredToken_returns401() throws Exception {
            // eyJ로 시작하지만 유효하지 않은 JWT
            String fakeToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwiZXhwIjoxfQ.invalid";

            mockMvc.perform(get("/explore")
                    .header("Authorization", "Bearer " + fakeToken))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.statusCode").value(40100));
        }
    }

    // ── 공개 API: 토큰 없이 접근 가능 ──────────────────

    @Nested
    @DisplayName("공개 API - 토큰 없이 접근 가능")
    class PublicEndpoints {

        @Test
        @DisplayName("POST /auth/login → 토큰 없이 접근 가능 (401 아닌 것만 확인)")
        void authLogin_withoutToken_notUnauthorized() throws Exception {
            // /auth/** 는 permitAll 이므로 401이 아닌 다른 응답(404 등)이 나와야 함
            mockMvc.perform(post("/auth/login")
                    .contentType("application/json")
                    .content("{\"email\":\"test@test.com\",\"password\":\"pw\"}"))
                    .andExpect(status().is(org.hamcrest.Matchers.not(401)));
        }

        @Test
        @DisplayName("GET /user/metadata → 토큰 없이 접근 가능")
        void userMetadata_withoutToken_notUnauthorized() throws Exception {
            mockMvc.perform(get("/user/metadata"))
                    .andExpect(status().is(org.hamcrest.Matchers.not(401)));
        }
    }
}
