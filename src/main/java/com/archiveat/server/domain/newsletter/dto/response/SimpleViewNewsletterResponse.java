package com.archiveat.server.domain.newsletter.dto.response;

import java.util.List;

public record SimpleViewNewsletterResponse(
                Long userNewsletterId,
                String categoryName,
                String topicName,
                String title,
                String thumbnailUrl,
                String domainName,
                String label,
                String memo,
                String contentUrl,
                String createdAt,
                boolean isRead,
                List<NewsletterSummaryBlock> newsletterSimpleSummary) {
}
