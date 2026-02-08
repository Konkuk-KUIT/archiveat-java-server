package com.archiveat.server.domain.newsletter.controller;

import com.archiveat.server.domain.newsletter.service.NewsletterQueueService;
import com.archiveat.server.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Newsletter Queue 모니터링 API
 * 
 * Redis Queue 상태를 확인할 수 있는 엔드포인트 제공
 */
@RestController
@RequestMapping("/admin/newsletter-queue")
@RequiredArgsConstructor
public class NewsletterQueueMonitorController {

    private final NewsletterQueueService queueService;

    /**
     * Queue 상태 조회
     * 
     * @return Queue 크기, DLQ 크기 등
     */
    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> getQueueStatus() {
        Long queueSize = queueService.getQueueSize();
        Long dlqSize = queueService.getDLQSize();

        return ApiResponse.ok(Map.of(
                "queueSize", queueSize != null ? queueSize : 0,
                "dlqSize", dlqSize != null ? dlqSize : 0,
                "status", queueSize != null && queueSize > 10 ? "HIGH_LOAD" : "NORMAL"));
    }
}
