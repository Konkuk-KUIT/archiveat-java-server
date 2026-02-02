package com.archiveat.server.domain.explore.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 특정 토픽의 뉴스레터 목록 조회를 위한 응답 DTO입니다.
 */
public record TopicNewslettersResponse(
        Long topicId,
        String topicName,
        boolean hasNext,
        List<NewsletterItemResponse> newsletters
) {
    /**
     * 목록에 표시될 개별 뉴스레터 요약 정보입니다.
     */
    public record NewsletterItemResponse(
            Long userNewsletterId,
            String title,
            String thumbnailUrl,
            boolean isRead,
            LocalDateTime createdAt
    ) {}
}
