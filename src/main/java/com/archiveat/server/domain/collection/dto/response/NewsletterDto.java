package com.archiveat.server.domain.collection.dto.response;

public record NewsletterDto(
        Long newsletterId,
        String domainName,
        String title,
        String thumbnailUrl,
        Integer consumptionTimeMin,
        String memo,
        Boolean isRead) {
}
