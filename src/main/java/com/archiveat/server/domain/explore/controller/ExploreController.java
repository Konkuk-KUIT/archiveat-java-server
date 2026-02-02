package com.archiveat.server.domain.explore.controller;

import com.archiveat.server.domain.explore.dto.response.ExploreResponse;
import com.archiveat.server.domain.explore.dto.response.TopicNewslettersResponse;
import com.archiveat.server.domain.explore.service.ExploreService;
import com.archiveat.server.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/explore")
public class ExploreController {

    private final ExploreService exploreService;

    /**
     * 사용자의 뉴스레터 보관함 상태와 카테고리별 통계를 조회합니다.
     */
    @GetMapping
    public ApiResponse<ExploreResponse> getExploreData(
            @AuthenticationPrincipal Long userId
    ) {
        ExploreResponse response = exploreService.getExploreData(userId);

        return ApiResponse.ok(response);
    }

    /**
     * 특정 토픽에 속한 뉴스레터 목록을 페이징하여 조회합니다.
     * * @param topicId 조회할 토픽의 식별자
     * @param pageable 페이징 파라미터 (page, size)
     */
    @GetMapping("/topic/{topicId}/user-newsletters")
    public ApiResponse<TopicNewslettersResponse> getTopicNewsletters(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long topicId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        TopicNewslettersResponse response = exploreService.getTopicNewsletters(userId, topicId, pageable);

        return ApiResponse.ok(response);
    }

}
