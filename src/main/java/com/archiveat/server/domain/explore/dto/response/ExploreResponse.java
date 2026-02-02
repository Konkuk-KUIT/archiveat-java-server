package com.archiveat.server.domain.explore.dto.response;

import com.archiveat.server.global.common.constant.LlmStatus;

import java.util.List;

public record ExploreResponse(
        int inboxCount,
        LlmStatus llmStatus,
        List<CategoryExploreResponse> categories
) {
    /**
     * 카테고리별 토픽 목록 정보를 담는 DTO
     */
    public record CategoryExploreResponse(
            Long id,
            String name,
            List<TopicExploreResponse> topics
    ) {}

    /**
     * 토픽별 뉴스레터 개수 정보를 담는 DTO
     */
    public record TopicExploreResponse(
            Long id,
            String name,
            long newsletterCount
    ) {}

}
