package com.archiveat.server.global.config;

import com.archiveat.server.domain.newsletter.repository.UserNewsletterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataMigrationRunner implements CommandLineRunner {

    private final UserNewsletterRepository userNewsletterRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        try {
            log.info("Starting data migration for UserNewsletter category/topic...");
            userNewsletterRepository.bulkMigrateCategoryAndTopic();
            log.info("Data migration completed.");
        } catch (Exception e) {
            log.error("Data migration failed", e);
            // Do not rethrow to prevent application startup failure
        }
    }
}
