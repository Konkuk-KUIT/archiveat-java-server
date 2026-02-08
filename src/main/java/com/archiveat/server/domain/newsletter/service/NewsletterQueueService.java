package com.archiveat.server.domain.newsletter.service;

import com.archiveat.server.domain.newsletter.dto.NewsletterJobMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Redis Queue 관리 서비스
 * 
 * - 작업 큐: newsletter:job:queue
 * - Dead Letter Queue: newsletter:job:dlq
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewsletterQueueService {

    private static final String QUEUE_KEY = "newsletter:job:queue";
    private static final String DLQ_KEY = "newsletter:job:dlq";

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 작업을 큐에 추가
     */
    public void enqueue(NewsletterJobMessage message) {
        redisTemplate.opsForList().leftPush(QUEUE_KEY, message);
        log.info("Enqueued newsletter job: newsletterId={}, contentUrl={}",
                message.newsletterId(), message.contentUrl());
    }

    /**
     * 큐에서 작업을 가져옴 (블로킹, BLPOP)
     * 
     * @param timeout 대기 시간 (초), 0이면 무한 대기
     * @return 작업 메시지 (큐가 비어있으면 대기)
     */
    public NewsletterJobMessage dequeue(long timeout) {
        try {
            log.debug("Attempting rightPop from Redis, timeout={}s", timeout);
            Object message = redisTemplate.opsForList().rightPop(QUEUE_KEY, timeout, TimeUnit.SECONDS);

            if (message instanceof NewsletterJobMessage jobMessage) {
                log.info("✅ Dequeued from Redis: newsletterId={}", jobMessage.newsletterId());
                return jobMessage;
            }

            if (message == null) {
                log.trace("rightPop returned null (timeout or empty queue)");
            } else {
                log.warn("Dequeued object is not NewsletterJobMessage: {}", message.getClass());
            }

            return null;
        } catch (Exception e) {
            log.error("Failed to dequeue from Redis", e);
            return null;
        }
    }

    /**
     * 실패한 작업을 Dead Letter Queue로 이동
     */
    public void moveToDLQ(NewsletterJobMessage message, String errorReason) {
        redisTemplate.opsForList().leftPush(DLQ_KEY, message);
        log.error("Moved to DLQ: newsletterId={}, retryCount={}, reason={}",
                message.newsletterId(), message.retryCount(), errorReason);
    }

    /**
     * 재시도를 위해 다시 큐에 추가
     */
    public void requeueForRetry(NewsletterJobMessage message) {
        NewsletterJobMessage retryMessage = message.withRetry();
        enqueue(retryMessage);
        log.warn("Requeued for retry: newsletterId={}, retryCount={}",
                retryMessage.newsletterId(), retryMessage.retryCount());
    }

    /**
     * 큐 크기 조회
     */
    public Long getQueueSize() {
        return redisTemplate.opsForList().size(QUEUE_KEY);
    }

    /**
     * DLQ 크기 조회
     */
    public Long getDLQSize() {
        return redisTemplate.opsForList().size(DLQ_KEY);
    }
}
