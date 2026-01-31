package com.archiveat.server.domain.auth.controller;

import com.archiveat.server.domain.auth.dto.request.EmailCheckRequest;
import com.archiveat.server.domain.auth.dto.request.LoginRequest;
import com.archiveat.server.domain.auth.dto.request.SignupRequest;
import com.archiveat.server.domain.auth.dto.response.LoginResponse;
import com.archiveat.server.domain.auth.service.AuthService;
import com.archiveat.server.global.common.response.ApiResponse;
import com.archiveat.server.global.common.response.SuccessCode;
import com.archiveat.server.global.jwt.RefreshTokenCookieProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    private final RefreshTokenCookieProvider refreshTokenCookieProvider;

    @PostMapping("/signup")
    public ApiResponse<LoginResponse> signup(
            @Valid @RequestBody SignupRequest signupRequest,
            HttpServletResponse response
    ){
        AuthService.IssuedTokens tokens = authService.signupAndLogin(
                signupRequest.getEmail(),
                signupRequest.getPassword(),
                signupRequest.getNickname()
        );

        // refresh는 쿠키로
        refreshTokenCookieProvider.set(response, tokens.refreshToken());

        // access는 바디로
        return ApiResponse.ok(
                SuccessCode.USER_CREATED,
                new LoginResponse(tokens.accessToken(), "Bearer")
        );
//
//        LoginResponse loginResponse = userService.signupAndLogin(
//          signupRequest.getEmail(),
//          signupRequest.getPassword(),
//          signupRequest.getNickname()
//        );
//
//        return ApiResponse.ok(SuccessCode.USER_CREATED, loginResponse);
    }

    @PostMapping("/check-email")
    public ApiResponse<Boolean> checkEmail(
            @Valid @RequestBody EmailCheckRequest emailCheckRequest
            ){
        boolean exists = authService.checkEmail(emailCheckRequest.getEmail());
        boolean available = !exists;

        return ApiResponse.ok(available);
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletResponse response
    ){
        AuthService.IssuedTokens tokens = authService.login(
                loginRequest.getEmail(),
                loginRequest.getPassword()
        );

        refreshTokenCookieProvider.set(response, tokens.refreshToken());

        return ApiResponse.ok(
                SuccessCode.SUCCESS,
                new LoginResponse(tokens.accessToken(), "Bearer")
        );
//        LoginResponse loginResponse = userService.login(
//                loginRequest.getEmail(),
//                loginRequest.getPassword()
//        );
//
//        return ApiResponse.ok(SuccessCode.SUCCESS, loginResponse);
    }

    // refresh로 access 재발급
    @PostMapping("/reissue")
    public ApiResponse<LoginResponse> refresh(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String refreshToken = refreshTokenCookieProvider.extract(request);
        AuthService.IssuedTokens tokens = authService.reissueTokensByRefresh(refreshToken);

        // rotation 적용했으니 쿠키 갱신
        refreshTokenCookieProvider.set(response, tokens.refreshToken());

        return ApiResponse.ok(
                SuccessCode.SUCCESS,
                new LoginResponse(tokens.accessToken(), "Bearer")
        );
    }

    // 로그아웃: user.refreshToken null + 쿠키 제거
    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @AuthenticationPrincipal Long userId,
            HttpServletResponse response
    ) {
        authService.logout(userId);
        refreshTokenCookieProvider.clear(response);
        return ApiResponse.ok();
    }
}
