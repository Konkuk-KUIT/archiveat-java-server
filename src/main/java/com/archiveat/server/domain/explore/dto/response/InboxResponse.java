package com.archiveat.server.domain.explore.dto.response;

import com.archiveat.server.global.common.constant.LlmStatus;
import lombok.Builder;

import java.util.List;

@Builder
public record InboxResponse(
        List<InboxDateGroupDto> inbox
) {
    @Builder
    public record InboxDateGroupDto(
            String date,
            List<InboxItemDto> items
    ) {}

    @Builder
    public record InboxItemDto(
            Long userNewsletterId,
            LlmStatus llmStatus,
            String contentUrl,
            String title,
            String domainName,
            String createdAt,
            CategoryDto category,
            TopicDto topic
    ) {}

    public record CategoryDto(Long id, String name) {}
    public record TopicDto(Long id, String name) {}
}
