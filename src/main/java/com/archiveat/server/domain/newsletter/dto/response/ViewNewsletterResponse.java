package com.archiveat.server.domain.newsletter.dto.response;

import java.util.List;

public record ViewNewsletterResponse(
        Long userNewsletterId,
        String categoryName,
        String topicName,
        String title,
        String thumbnailUrl,
        String label,
        String memo,
        String contentUrl,
        List<NewsletterSummaryBlock> newsletterSummary
) {
}
