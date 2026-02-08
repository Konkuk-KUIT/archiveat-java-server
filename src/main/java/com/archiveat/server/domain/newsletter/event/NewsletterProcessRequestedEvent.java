package com.archiveat.server.domain.newsletter.event;

/**
 * Newsletter 처리 요청 이벤트
 * 
 * ⭐ 트랜잭션 분리를 위한 이벤트
 * - generateNewsletter()에서 발행
 * - EventListener가 트랜잭션 커밋 후 Redis Queue에 추가
 * - 이를 통해 queueSize=0 문제 해결
 */
public record NewsletterProcessRequestedEvent(
        Long newsletterId,
        String contentUrl) {
}