package com.archiveat.server.domain.explore.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record InboxEditResponse(
        CurrentInfoDto current,
        List<CategoryDto> categories,
        List<TopicDto> topics
) {
    @Builder
    public record CurrentInfoDto(
            Long userNewsletterId,
            Long categoryId,
            Long topicId,
            String memo
    ) {}

    public record CategoryDto(Long id, String name) {}

    public record TopicDto(
            Long id,
            Long categoryId,
            String name
    ) {}
}
