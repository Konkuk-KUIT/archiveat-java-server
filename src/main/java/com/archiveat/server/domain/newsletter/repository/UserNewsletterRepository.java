package com.archiveat.server.domain.newsletter.repository;

import com.archiveat.server.domain.newsletter.entity.UserNewsletter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserNewsletterRepository extends JpaRepository<UserNewsletter, Long> {
    List<UserNewsletter> findByUserIdAndNewsletterIdIn(Long userId, List<Long> newsletterIds);

    List<UserNewsletter> findAllByUserId(Long userId);

    // 주간 리포트: 기간 내 저장된 뉴스레터
    List<UserNewsletter> findByUserIdAndCreatedAtBetween(Long userId, LocalDateTime start, LocalDateTime end);

    // 주간 리포트: 기간 내 읽은 뉴스레터
    List<UserNewsletter> findByUserIdAndLastViewedAtBetweenAndIsReadTrue(Long userId, LocalDateTime start,
            LocalDateTime end);

    // 최근 읽은 뉴스레터 목록 (정렬)
    List<UserNewsletter> findByUserIdAndIsReadTrueOrderByLastViewedAtDesc(Long userId);
}
