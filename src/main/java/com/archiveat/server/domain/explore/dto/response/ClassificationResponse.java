package com.archiveat.server.domain.explore.dto.response;

import lombok.Builder;

@Builder
public record ClassificationResponse(
        Long userNewsletterId,
        Long newsletterId,
        CategoryDto category,
        TopicDto topic,
        String memo,
        String classificationConfirmedAt,
        String modifiedAt
) {
    public record CategoryDto(Long id, String name) {}
    public record TopicDto(Long id, String name) {}
}
