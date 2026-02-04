package com.archiveat.server.domain.explore.dto.request;

public record ClassificationRequest(
        Long categoryId,
        Long topicId,
        String memo
) {}
