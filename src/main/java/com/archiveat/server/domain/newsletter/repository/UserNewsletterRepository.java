package com.archiveat.server.domain.newsletter.repository;

import com.archiveat.server.domain.newsletter.entity.UserNewsletter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserNewsletterRepository extends JpaRepository<UserNewsletter, Long> {
    // 유저별 뉴스레터 목록 조회 (최신순 등 정렬 조건 추가 가능)
    List<UserNewsletter> findAllByUserId(Long userId);
}
