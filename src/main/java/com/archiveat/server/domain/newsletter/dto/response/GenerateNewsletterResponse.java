package com.archiveat.server.domain.newsletter.dto.response;

public record GenerateNewsletterResponse(
        Long newsletterId,
        String llmStatus
) {
}
