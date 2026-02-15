package com.archiveat.server.domain.newsletter.repository;

import com.archiveat.server.domain.newsletter.entity.UserNewsletter;
import com.archiveat.server.global.common.constant.DepthType;
import com.archiveat.server.global.common.constant.LlmStatus;
import com.archiveat.server.domain.user.entity.User;
import com.archiveat.server.domain.newsletter.entity.Newsletter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

        @Query("SELECT un FROM UserNewsletter un " +
                        "JOIN FETCH un.newsletter n " +
                        "LEFT JOIN FETCH un.category " +
                        "LEFT JOIN FETCH un.topic " +
                        "WHERE un.id = :id AND un.user.id = :userId")
        Optional<UserNewsletter> findByIdAndUser_Id(@Param("id") Long id, @Param("userId") Long userId);

        // 중복 뉴스레터 체크
        boolean existsByUserAndNewsletter(User user, Newsletter newsletter);

        // 주간 리포트: 기간 내 저장된 뉴스레터
        List<UserNewsletter> findByUserIdAndCreatedAtBetween(Long userId, LocalDateTime start, LocalDateTime end);

        // 주간 리포트: 기간 내 읽은 뉴스레터
        List<UserNewsletter> findByUserIdAndLastViewedAtBetweenAndIsReadTrue(Long userId, LocalDateTime start,
                        LocalDateTime end);

        // 최근 읽은 뉴스레터 목록 (정렬)
        List<UserNewsletter> findByUserIdAndIsReadTrueOrderByLastViewedAtDesc(Long userId);

        // Newsletter에 연결된 모든 UserNewsletter 조회 (Label 업데이트용)
        List<UserNewsletter> findAllByNewsletter_Id(Long newsletterId);

        @Query("SELECT un.topic.id, COUNT(un.id) FROM UserNewsletter un " +
                        "WHERE un.user.id = :userId " +
                        "GROUP BY un.topic.id")
        List<Object[]> countNewslettersByTopicForUser(@Param("userId") Long userId);

        // 인박스(미확인) 뉴스레터 개수 조회
        int countByUserIdAndIsConfirmedFalse(Long userId);

        /**
         * 특정 유저의 특정 토픽에 속한 뉴스레터 목록을 최신순으로 페이징 조회
         * N+1 문제는 hibernate.default_batch_fetch_size 설정으로 완화 (연관 객체 접근 시 추가 쿼리 발생 가능)
         */
        @Query("SELECT un FROM UserNewsletter un " +
                        "WHERE un.user.id = :userId AND un.topic.id = :topicId " +
                        "ORDER BY un.createdAt DESC")
        Slice<UserNewsletter> findByUserIdAndTopicId(
                        @Param("userId") Long userId,
                        @Param("topicId") Long topicId,
                        Pageable pageable);

        /**
         * 유저의 인박스 아이템(isConfirmed = false)을 최신순으로 조회
         * Fetch Join을 사용하여 Newsletter와 그에 연결된 Domain 정보를 한 번에 로딩 (N+1 문제 방지)
         */
        @Query("SELECT un FROM UserNewsletter un " +
                        "JOIN FETCH un.newsletter n " +
                        "LEFT JOIN FETCH n.domain d " +
                        "LEFT JOIN FETCH un.category " +
                        "LEFT JOIN FETCH un.topic " +
                        "WHERE un.user.id = :userId AND un.isConfirmed = false " +
                        "ORDER BY un.createdAt DESC")
        List<UserNewsletter> findAllInboxByUserId(@Param("userId") Long userId);

        /**
         * 특정 유저의 인박스 아이템들을 일괄 확인 처리
         */
        @Modifying(clearAutomatically = true)
        @Query("UPDATE UserNewsletter un SET un.isConfirmed = true, un.confirmedAt = :now " +
                        "WHERE un.user.id = :userId " +
                        "AND un.isConfirmed = false " +
                        "AND un.newsletter.llmStatus = :status") // [Insight] 서브쿼리 없이 직접 참조 가능
        void bulkConfirmByUserId(
                        @Param("userId") Long userId,
                        @Param("now") LocalDateTime now,
                        @Param("status") LlmStatus status // [Reason] 하드코딩 방지 및 타입 안정성 확보
        );

        @Query("SELECT un FROM UserNewsletter un " +
                        "LEFT JOIN CollectionNewsletter cn ON cn.newsletter.id = un.newsletter.id " +
                        "AND cn.collection.user.id = un.user.id " +
                        "WHERE un.user.id = :userId " +
                        "AND un.depthType = :depthType " +
                        "AND cn.id IS NULL")
        List<UserNewsletter> findUncollectedNewsletters(@Param("userId") Long userId,
                        @Param("depthType") DepthType depthType);

        boolean existsByUserIdAndNewsletter_LlmStatus(Long userId, LlmStatus llmStatus);

        @Modifying(clearAutomatically = true)
        @Query(value = "UPDATE user_newsletters " +
                        "SET category_id = (SELECT c.id FROM newsletters n JOIN categories c ON c.name = n.category WHERE n.id = user_newsletters.newsletter_id), "
                        +
                        "topic_id = (SELECT t.id FROM newsletters n JOIN topics t ON t.name = n.topic WHERE n.id = user_newsletters.newsletter_id) "
                        +
                        "WHERE category_id IS NULL " +
                        "AND EXISTS (SELECT 1 FROM newsletters n JOIN categories c ON c.name = n.category JOIN topics t ON t.name = n.topic WHERE n.id = user_newsletters.newsletter_id)", nativeQuery = true)
        void bulkMigrateCategoryAndTopic();
}
