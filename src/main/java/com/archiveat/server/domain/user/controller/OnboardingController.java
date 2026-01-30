package com.archiveat.server.domain.user.controller;

import com.archiveat.server.domain.user.dto.request.NicknameRequest;
import com.archiveat.server.domain.user.dto.request.OnboardingInfoRequest;
import com.archiveat.server.domain.user.dto.response.NicknameResponse;
import com.archiveat.server.domain.user.dto.response.OnboardingMetadataResponse;
import com.archiveat.server.domain.user.service.OnboardingService;
import com.archiveat.server.global.common.response.ApiResponse;
import com.archiveat.server.global.common.response.SuccessCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/user")
public class OnboardingController {
    private final OnboardingService onboardingService;

    @PostMapping("/nickname")
    public ApiResponse<Void> editNickname(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody NicknameRequest request
    ) {
        onboardingService.editNickname(userId, request.getNickname());

        return ApiResponse.ok(null);
    }

    @GetMapping("/nickname")
    public ApiResponse<NicknameResponse> nickname(
            @AuthenticationPrincipal Long userId
    ) {
        NicknameResponse nicknameResponse = onboardingService.getNickname(userId);

        return ApiResponse.ok(SuccessCode.SUCCESS, nicknameResponse);
    }

    @GetMapping("/metadata")
    public ApiResponse<OnboardingMetadataResponse> getOnboardingMetadata() {
        OnboardingMetadataResponse metadataResponse = onboardingService.getOnboardingMetadata();

        return ApiResponse.ok(metadataResponse);
    }

    @PostMapping("/metadata")
    public ApiResponse<Void> submitOnboardingInfo(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody OnboardingInfoRequest request
    ) {
        onboardingService.submitOnboardingInfo(userId, request);

        return ApiResponse.ok(null);
    }
}
