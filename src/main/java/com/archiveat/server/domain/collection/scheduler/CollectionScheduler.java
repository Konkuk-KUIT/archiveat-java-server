package com.archiveat.server.domain.collection.scheduler;

import com.archiveat.server.domain.collection.service.CollectionGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class CollectionScheduler {

    private final CollectionGeneratorService collectionGeneratorService;

    /**
     * 매일 06:00, 12:00, 18:00, 22:00에 컬렉션 생성 로직 실행
     */
    @Scheduled(cron = "0 0 6,12,18,22 * * *", zone = "Asia/Seoul")
    public void scheduleCollectionGeneration() {
        LocalTime now = LocalTime.now(java.time.ZoneId.of("Asia/Seoul"));
        log.info("Scheduled collection generation triggered at {}", now);

        try {
            collectionGeneratorService.generateCollectionsForTime(now);
        } catch (Exception e) {
            log.error("Error during scheduled collection generation", e);
        }
    }

    /**
     * 시연용 임시 스케줄링 (16:30)
     */
    @Scheduled(cron = "0 30 16 * * *", zone = "Asia/Seoul")
    public void scheduleDemoCollectionGeneration() {
        scheduleCollectionGeneration();
    }
}
