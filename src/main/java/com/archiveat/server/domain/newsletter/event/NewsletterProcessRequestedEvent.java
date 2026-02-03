package com.archiveat.server.domain.newsletter.event;

public record NewsletterProcessRequestedEvent(Long newsletterId, String contentUrl) {
}