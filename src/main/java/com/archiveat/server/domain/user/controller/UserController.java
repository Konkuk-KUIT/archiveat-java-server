package com.archiveat.server.domain.user.controller;


import com.archiveat.server.domain.user.dto.request.EmailCheckRequest;
import com.archiveat.server.domain.user.dto.request.LoginRequest;
import com.archiveat.server.domain.user.dto.request.SignupRequest;
import com.archiveat.server.domain.user.dto.response.LoginResponse;
import com.archiveat.server.domain.user.model.User;
import com.archiveat.server.domain.user.service.UserService;
import com.archiveat.server.global.common.BaseEntity;
import com.archiveat.server.global.common.response.ApiResponse;
import com.archiveat.server.global.common.response.SuccessCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class UserController {
    private final UserService userService;

    @PostMapping("/signup")
    public ApiResponse<LoginResponse> signup(
            @Valid @RequestBody SignupRequest signupRequest
    ){
        //db에 저장
        LoginResponse loginResponse = userService.signupAndLogin(
          signupRequest.getEmail(),
          signupRequest.getPassword(),
          signupRequest.getNickname()
        );

        return ApiResponse.ok(SuccessCode.USER_CREATED, loginResponse);
    }

    @PostMapping("/check-email")
    public ApiResponse<Boolean> checkEmail(
            @Valid @RequestBody EmailCheckRequest emailCheckRequest
            ){
        boolean exists = userService.checkEmail(emailCheckRequest.getEmail());
        boolean available = !exists;

        return ApiResponse.ok(available);
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest httpRequest
    ){
        LoginResponse loginResponse = userService.login(
                loginRequest.getEmail(),
                loginRequest.getPassword()
        );

        return ApiResponse.ok(SuccessCode.SUCCESS, loginResponse);
    }
}
