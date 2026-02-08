package com.archiveat.server.domain.newsletter.event;

import com.archiveat.server.domain.newsletter.dto.NewsletterJobMessage;
import com.archiveat.server.domain.newsletter.service.NewsletterQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Newsletter 이벤트 리스너
 * 
 * ⭐ 트랜잭션 문제 해결:
 * - @Transactional 안에서 직접 Redis에 작업을 넣으면 커밋 전까지 반영되지 않음
 * - AFTER_COMMIT 단계에서 실행되므로 DB 트랜잭션이 완전히 끝난 후 Redis에 추가됨
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NewsletterEventListener {

    private final NewsletterQueueService queueService;

    /**
     * Newsletter 생성 이벤트 처리
     * 
     * AFTER_COMMIT: DB 트랜잭션이 완전히 커밋된 후 실행
     * → Redis에 즉시 반영됨 (queueSize가 정확하게 표시됨)
     */
    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNewsletterProcess(NewsletterProcessRequestedEvent event) {
        NewsletterJobMessage message = NewsletterJobMessage.create(
                event.newsletterId(),
                event.contentUrl());

        queueService.enqueue(message);
        log.info("✅ Newsletter job enqueued after transaction commit: newsletterId={}, queueSize={}",
                event.newsletterId(), queueService.getQueueSize());
    }
}