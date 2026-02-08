package com.archiveat.server.domain.newsletter.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Redis Queue에 저장될 작업 메시지
 * 
 * @param newsletterId Newsletter ID
 * @param contentUrl   콘텐츠 URL
 * @param enqueuedAt   큐에 추가된 시각
 * @param retryCount   재시도 횟수 (최대 3회)
 */
public record NewsletterJobMessage(
        Long newsletterId,
        String contentUrl,
        LocalDateTime enqueuedAt,
        int retryCount) implements Serializable {

    /**
     * 새 작업 메시지 생성 (retryCount = 0)
     */
    public static NewsletterJobMessage create(Long newsletterId, String contentUrl) {
        return new NewsletterJobMessage(newsletterId, contentUrl, LocalDateTime.now(), 0);
    }

    /**
     * 재시도 메시지 생성
     */
    public NewsletterJobMessage withRetry() {
        return new NewsletterJobMessage(newsletterId, contentUrl, enqueuedAt, retryCount + 1);
    }

    /**
     * 최대 재시도 횟수에 도달했는지 확인
     */
    @JsonIgnore
    public boolean isMaxRetryReached() {
        return retryCount >= 3;
    }
}
