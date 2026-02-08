package com.archiveat.server.domain.newsletter.worker;

import com.archiveat.server.domain.newsletter.dto.NewsletterJobMessage;
import com.archiveat.server.domain.newsletter.service.NewsletterQueueService;
import com.archiveat.server.domain.newsletter.service.NewsletterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Newsletter 비동기 처리 Worker
 * 
 * - ApplicationRunner 구현으로 앱 시작 시 자동 실행
 * - Redis BLPOP을 사용하여 큐가 비어있으면 대기
 * - 작업이 들어오면 즉시 처리 (0ms 지연)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NewsletterWorker implements ApplicationRunner {

    private final NewsletterQueueService queueService;
    private final NewsletterService newsletterService;

    private volatile boolean running = true;

    @Override
    public void run(ApplicationArguments args) {
        log.info("🚀 Starting NewsletterWorker...");

        // 별도 스레드에서 무한 루프 실행
        Thread workerThread = new Thread(() -> {
            log.info("📨 Worker thread started, waiting for jobs...");
            while (running) {
                try {
                    // BLPOP: 큐에서 데이터 가져오기 (5초 timeout, 0은 일부 환경에서 문제 발생 가능)
                    log.debug("Attempting to dequeue from Redis...");
                    NewsletterJobMessage message = queueService.dequeue(5);

                    if (message != null) {
                        log.info("✅ Dequeued job from Redis: newsletterId={}, contentUrl={}",
                                message.newsletterId(), message.contentUrl());
                        processMessage(message);
                    } else {
                        // timeout 후 null이면 정상 (다시 대기)
                        log.trace("No message in queue after 5s timeout, retrying...");
                    }
                } catch (Exception e) {
                    log.error("❌ Worker error, will retry in 1 second", e);
                    // 에러 발생해도 worker는 계속 실행
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            log.info("Worker thread stopped");
        }, "newsletter-worker");

        workerThread.setDaemon(false); // 데몬 스레드가 아니므로 앱 종료 시까지 실행
        workerThread.start();

        log.info("✅ NewsletterWorker started successfully");
    }

    /**
     * 메시지 처리
     */
    private void processMessage(NewsletterJobMessage message) {
        log.info("Processing newsletter job: newsletterId={}, retryCount={}",
                message.newsletterId(), message.retryCount());

        try {
            // 실제 Newsletter 처리
            newsletterService.processNewsletterAsync(message.newsletterId(), message.contentUrl());

        } catch (Exception e) {
            log.error("Failed to process newsletter: newsletterId={}, retryCount={}",
                    message.newsletterId(), message.retryCount(), e);

            // 재시도 로직
            if (message.isMaxRetryReached()) {
                // 3회 실패 시 DLQ로 이동
                queueService.moveToDLQ(message, e.getMessage());
            } else {
                // 재시도
                queueService.requeueForRetry(message);
            }
        }
    }

    /**
     * Worker 종료 (Graceful Shutdown)
     */
    public void stop() {
        log.info("Stopping NewsletterWorker...");
        running = false;
    }
}
