package com.archiveat.server.domain.newsletter.event;

import com.archiveat.server.domain.newsletter.service.NewsletterService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NewsletterEventListener {

    private final NewsletterService newsletterService;

    @Async("taskExecutor") // 여기서 별도 스레드로 분기
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) // 커밋이 완료된 후 실행 보장
    public void handleNewsletterProcess(NewsletterProcessRequestedEvent event) {
        newsletterService.processNewsletterAsync(event.newsletterId(), event.contentUrl());
    }
}