package com.archiveat.server.domain.user.dto.response;

import java.util.List;

public record OnboardingMetadataResponse(
        List<String> employmentTypes,
        List<String> availabilityOptions,
        List<CategoryMetadataResponse> categories
) {
    public record CategoryMetadataResponse(
            Long id,
            String name,
            List<TopicMetadataResponse> topics
    ) {}

    public record TopicMetadataResponse(
            Long id,
            String name
    ) {}
}