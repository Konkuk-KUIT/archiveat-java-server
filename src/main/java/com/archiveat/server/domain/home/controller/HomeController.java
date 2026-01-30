package com.archiveat.server.domain.home.controller;

import com.archiveat.server.domain.home.dto.response.HomeResponse;
import com.archiveat.server.domain.home.service.HomeService;
import com.archiveat.server.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/home")
public class HomeController {

    private final HomeService homeService;

    /**
     * 유저의 홈 화면 데이터를 조회합니다.
     * @param userId 인증 필터(JwtAuthenticationFilter)를 통해 추출된 유저 식별자
     */
    @GetMapping
    public ApiResponse<HomeResponse> getHomeData(
            @AuthenticationPrincipal Long userId
    ) {
        HomeResponse homeResponse = homeService.getHomeData(userId);

        return ApiResponse.ok(homeResponse);
    }
}
