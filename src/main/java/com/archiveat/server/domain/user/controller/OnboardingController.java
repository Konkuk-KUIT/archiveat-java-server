package com.archiveat.server.domain.user.controller;

import com.archiveat.server.domain.user.dto.request.NicknameRequest;
import com.archiveat.server.domain.user.service.UserService;
import com.archiveat.server.global.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/user")
public class OnboardingController {
    private final UserService userService;

    @PostMapping("/nickname")
    public ApiResponse<Void> editNickname(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody NicknameRequest request
    ){
        userService.editNickname(userId, request.getNickname());

        return ApiResponse.ok(null);
    }
}
