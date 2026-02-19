package com.archiveat.server.domain.auth.controller;

import com.archiveat.server.domain.auth.dto.request.LoginRequest;
import com.archiveat.server.domain.auth.service.AuthService;
import com.archiveat.server.global.jwt.RefreshTokenCookieProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private RefreshTokenCookieProvider refreshTokenCookieProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("로그인 성공: 올바른 요청 시 200 OK와 토큰을 반환한다")
    void login_Success() throws Exception {
        // given
        LoginRequest request = new LoginRequest("test@example.com", "password123");
        AuthService.IssuedTokens tokens = new AuthService.IssuedTokens("access", "refresh");

        given(authService.login(eq("test@example.com"), eq("password123"))).willReturn(tokens);

        // when & then
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("access"))
                .andExpect(jsonPath("$.isSuccess").value(true));

        verify(authService).login(eq("test@example.com"), eq("password123"));
        verify(refreshTokenCookieProvider).set(any(), eq("refresh"));
    }

    @Test
    @DisplayName("로그인 실패: 이메일 형식이 올바르지 않으면 400 에러를 반환한다")
    void login_Fail_InvalidEmail() throws Exception {
        // given
        // 잘못된 이메일 형식 ("invalid-email")
        LoginRequest request = new LoginRequest("invalid-email", "password123");

        // when & then
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()); // 400 Bad Request 기대
    }
}