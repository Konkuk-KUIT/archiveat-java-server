package com.archiveat.server.domain.newsletter.repository;

import com.archiveat.server.domain.newsletter.entity.UserNewsletter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserNewsletterRepository extends JpaRepository<UserNewsletter, Long> {
    List<UserNewsletter> findByUserIdAndNewsletterIdIn(Long userId, List<Long> newsletterIds);

    List<UserNewsletter> findAllByUserId(Long userId);

    int deleteByIdAndUser_Id(Long id, Long userId);

    Optional<UserNewsletter> findByIdAndUser_Id(Long id, Long userId);

    // 주간 리포트: 기간 내 저장된 뉴스레터
    List<UserNewsletter> findByUserIdAndCreatedAtBetween(Long userId, LocalDateTime start, LocalDateTime end);

    // 주간 리포트: 기간 내 읽은 뉴스레터
    List<UserNewsletter> findByUserIdAndLastViewedAtBetweenAndIsReadTrue(Long userId, LocalDateTime start,
                                                                         LocalDateTime end);

    // 최근 읽은 뉴스레터 목록 (정렬)
    List<UserNewsletter> findByUserIdAndIsReadTrueOrderByLastViewedAtDesc(Long userId);

    // Newsletter에 연결된 모든 UserNewsletter 조회 (Label 업데이트용)
    List<UserNewsletter> findAllByNewsletter_Id(Long newsletterId);

    @Query("SELECT tn.topic.id, COUNT(un.id) FROM UserNewsletter un " +
            "JOIN TopicNewsletter tn ON un.newsletter.id = tn.newsletter.id " +
            "WHERE un.user.id = :userId " +
            "GROUP BY tn.topic.id")
    List<Object[]> countNewslettersByTopicForUser(@Param("userId") Long userId);

    // 인박스(미확인) 뉴스레터 개수 조회
    int countByUserIdAndIsConfirmedFalse(Long userId);

    /**
     * 특정 유저의 특정 토픽에 속한 뉴스레터 목록을 최신순으로 페이징 조회
     * N+1 문제를 방지하기 위해 Newsletter 엔티티를 FETCH JOIN
     */
    @Query("SELECT un FROM UserNewsletter un " +
            "JOIN FETCH un.newsletter " +
            "JOIN TopicNewsletter tn ON un.newsletter.id = tn.newsletter.id " +
            "WHERE un.user.id = :userId AND tn.topic.id = :topicId " +
            "ORDER BY un.createdAt DESC")
    Slice<UserNewsletter> findByUserIdAndTopicId(
            @Param("userId") Long userId,
            @Param("topicId") Long topicId,
            Pageable pageable
    );
}
