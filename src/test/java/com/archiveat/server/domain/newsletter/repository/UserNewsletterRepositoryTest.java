package com.archiveat.server.domain.newsletter.repository;

import com.archiveat.server.domain.explore.entity.Category;
import com.archiveat.server.domain.explore.entity.Topic;
import com.archiveat.server.domain.explore.entity.TopicNewsletter;
import com.archiveat.server.domain.newsletter.entity.Newsletter;
import com.archiveat.server.domain.newsletter.entity.UserNewsletter;
import com.archiveat.server.domain.user.entity.User;
import com.archiveat.server.global.common.constant.LlmStatus;
import com.archiveat.server.global.config.JpaConfig;
import com.archiveat.server.global.config.TestConfig;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({ JpaConfig.class, TestConfig.class })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserNewsletterRepositoryTest {

        @Autowired
        private UserNewsletterRepository userNewsletterRepository;
        @Autowired
        private TestEntityManager em;

        @Test
        @DisplayName("[UserNewsletter] 유저별 토픽 뉴스레터 개수 조회 (GROUP BY 쿼리 검증)")
        void countNewslettersByTopicForUser_Success() {
                // given
                User user = User.builder()
                                .email("test@test.com")
                                .nickname("아카이빗")
                                .build();
                em.persist(user);

                Category cat = Category.builder()
                                .name("IT")
                                .build();
                em.persist(cat);

                Topic topic = Topic.builder()
                                .name("AI")
                                .category(cat)
                                .build();
                em.persist(topic);

                Newsletter newsletter = Newsletter.builder()
                                .title("뉴스레터1")
                                .contentUrl("https://link.com/1")
                                .build();
                em.persist(newsletter);

                TopicNewsletter tn = TopicNewsletter.builder()
                                .topic(topic)
                                .newsletter(newsletter)
                                .build();
                em.persist(tn);

                UserNewsletter un = UserNewsletter.create(user, newsletter, cat, topic, "메모");
                em.persist(un);

                em.flush();
                em.clear();

                // when
                List<Object[]> results = userNewsletterRepository.countNewslettersByTopicForUser(user.getId());

                // then
                assertThat(results).isNotEmpty();
                Object[] row = results.getFirst();
                assertThat(row[0]).isEqualTo(topic.getId());
                assertThat(row[1]).isEqualTo(1L);
        }

        @Test
        @DisplayName("[UserNewsletter] 미확인 뉴스레터(인박스) 목록 조회 검증")
        void findAllInboxByUserId_Success() {
                // given
                User user = User.builder().email("inbox@test.com").nickname("인박스유저").build();
                em.persist(user);

                Newsletter n1 = Newsletter.builder().title("뉴스레터1").contentUrl("url1").build();
                Newsletter n2 = Newsletter.builder().title("뉴스레터2").contentUrl("url2").build();
                em.persist(n1);
                em.persist(n2);

                // n1은 확인 완료(isConfirmed=true), n2는 미확인(isConfirmed=false)으로 생성
                UserNewsletter un1 = UserNewsletter.create(user, n1, null, null, "확인함");
                un1.updateClassification(null, null, "분류확정");

                UserNewsletter un2 = UserNewsletter.create(user, n2, null, null, "미확인");

                em.persist(un1);
                em.persist(un2);
                em.flush();
                em.clear();

                // when
                List<UserNewsletter> inboxList = userNewsletterRepository.findAllInboxByUserId(user.getId());

                // then
                assertThat(inboxList).hasSize(1);
                assertThat(inboxList.getFirst().getNewsletter().getTitle()).isEqualTo("뉴스레터2");
        }

        @Test
        @DisplayName("[UserNewsletter] 인박스 아이템 일괄 확인 처리(Bulk Update) 검증")
        void bulkConfirmByUserId_Success() {
                // given
                User user = User.builder().email("bulk@test.com").nickname("벌크유저").build();
                em.persist(user);

                // 조건: llmStatus가 DONE인 것만 벌크 업데이트 대상임 (Repository 쿼리 기준)
                Newsletter n = Newsletter.builder().title("분석완료").contentUrl("done-url").llmStatus(LlmStatus.DONE)
                                .build();
                em.persist(n);

                UserNewsletter un = UserNewsletter.create(user, n, null, null, "벌크테스트");
                em.persist(un);
                em.flush();
                em.clear();

                // when
                userNewsletterRepository.bulkConfirmByUserId(user.getId(), LocalDateTime.now(), LlmStatus.DONE);
                em.flush();
                em.clear();

                // then
                UserNewsletter updatedUn = userNewsletterRepository.findById(un.getId()).get();
                assertThat(updatedUn.isConfirmed()).isTrue();
        }

        @Test
        @DisplayName("[Isolation] User A updates classification, User B remains unaffected")
        void testClassificationIsolation() {
                // given
                User userA = User.builder().email("a@test.com").nickname("UserA").build();
                User userB = User.builder().email("b@test.com").nickname("UserB").build();
                em.persist(userA);
                em.persist(userB);

                Category catIT = Category.builder().name("IT").build();
                Category catEco = Category.builder().name("Economy").build();
                em.persist(catIT);
                em.persist(catEco);

                Topic topicAI = Topic.builder().name("AI").category(catIT).build();
                Topic topicStock = Topic.builder().name("Stock").category(catEco).build();
                em.persist(topicAI);
                em.persist(topicStock);

                Newsletter newsletter = Newsletter.builder()
                                .title("Shared Newsletter")
                                .contentUrl("http://shared.com")
                                .build();
                newsletter.updateCategoryAndTopic("IT", "AI"); // Set initial metadata
                em.persist(newsletter);

                // Both users start with IT/AI
                UserNewsletter unA = UserNewsletter.create(userA, newsletter, catIT, topicAI, "Memo A");
                UserNewsletter unB = UserNewsletter.create(userB, newsletter, catIT, topicAI, "Memo B");
                em.persist(unA);
                em.persist(unB);

                em.flush();
                em.clear();

                // when: User A updates to Economy/Stock
                UserNewsletter loadedUnA = userNewsletterRepository.findById(unA.getId()).get();
                loadedUnA.updateClassification(catEco, topicStock, "Updated Memo A");
                em.flush();
                em.clear();

                // then: User B should still be IT/AI
                UserNewsletter loadedUnB = userNewsletterRepository.findById(unB.getId()).get();
                assertThat(loadedUnB.getCategory().getName()).isEqualTo("IT");
                assertThat(loadedUnB.getTopic().getName()).isEqualTo("AI");

                // then: User A should be Economy/Stock
                UserNewsletter reloadedUnA = userNewsletterRepository.findById(unA.getId()).get();
                assertThat(reloadedUnA.getCategory().getName()).isEqualTo("Economy");
                assertThat(reloadedUnA.getTopic().getName()).isEqualTo("Stock");

                // then: Original Newsletter should still indicate IT/AI (if it has fields for
                // it)
                Newsletter loadedNewsletter = em.find(Newsletter.class, newsletter.getId());
                assertThat(loadedNewsletter.getCategory()).isEqualTo("IT");
                assertThat(loadedNewsletter.getTopic()).isEqualTo("AI");
        }

        @Test
        @DisplayName("[Migration] Bulk update category/topic from Newsletter to UserNewsletter")
        void testBulkMigrateCategoryAndTopic() {
                // given
                User user = User.builder().email("migrate@test.com").nickname("MigrateUser").build();
                em.persist(user);

                Category cat = Category.builder().name("Economy").build();
                em.persist(cat);
                Topic topic = Topic.builder().name("Stock").category(cat).build();
                em.persist(topic);

                // Newsletter has category/topic names
                Newsletter newsletter = Newsletter.builder()
                                .title("Old Newsletter")
                                .contentUrl("http://old.com")
                                .build();
                newsletter.updateCategoryAndTopic("Economy", "Stock");
                em.persist(newsletter);

                // UserNewsletter has NULL category/topic
                UserNewsletter un = UserNewsletter.create(user, newsletter, null, null, "Old Memo");
                em.persist(un);

                em.flush();
                em.clear();

                // when
                userNewsletterRepository.bulkMigrateCategoryAndTopic();
                em.flush();
                em.clear();

                // then
                UserNewsletter migratedUn = userNewsletterRepository.findById(un.getId()).get();
                assertThat(migratedUn.getCategory()).isNotNull();
                assertThat(migratedUn.getCategory().getName()).isEqualTo("Economy");
                assertThat(migratedUn.getTopic()).isNotNull();
                assertThat(migratedUn.getTopic().getName()).isEqualTo("Stock");
        }
}