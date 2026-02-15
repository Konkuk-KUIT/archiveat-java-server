package com.archiveat.server.domain.report.service;

import com.archiveat.server.domain.newsletter.entity.Newsletter;
import com.archiveat.server.domain.newsletter.entity.UserNewsletter;
import com.archiveat.server.domain.newsletter.repository.UserNewsletterRepository;
import com.archiveat.server.domain.report.dto.response.ConsumptionResponse;
import com.archiveat.server.domain.report.dto.response.WeeklyReportResponse;
import com.archiveat.server.domain.user.entity.User;
import com.archiveat.server.domain.explore.entity.Topic;
import com.archiveat.server.global.common.constant.DepthType;
import com.archiveat.server.global.common.constant.PerspectiveType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

        @InjectMocks
        private ReportService reportService;

        @Mock
        private UserNewsletterRepository userNewsletterRepository;

        @Test
        @DisplayName("주간 리포트 생성 - 기본 데이터 검증")
        void getWeeklyReport() {
                // given
                Long userId = 1L;
                List<UserNewsletter> savedList = new ArrayList<>();
                List<UserNewsletter> readList = new ArrayList<>();

                // Mock UserNewsletter (Reflection used as fields are private/protected)
                UserNewsletter un1 = new UserNewsletter(null, null, null, null, null);
                ReflectionTestUtils.setField(un1, "depthType", DepthType.LIGHT);
                ReflectionTestUtils.setField(un1, "perspectiveType", PerspectiveType.NOW);
                savedList.add(un1);

                UserNewsletter un2 = new UserNewsletter(null, null, null, null, null);
                ReflectionTestUtils.setField(un2, "depthType", DepthType.DEEP);
                ReflectionTestUtils.setField(un2, "perspectiveType", PerspectiveType.FUTURE);
                readList.add(un2);

                when(userNewsletterRepository.findByUserIdAndCreatedAtBetween(eq(userId), any(), any()))
                                .thenReturn(savedList);
                when(userNewsletterRepository.findByUserIdAndLastViewedAtBetweenAndIsReadTrue(eq(userId), any(), any()))
                                .thenReturn(readList);

                // when
                WeeklyReportResponse response = reportService.getWeeklyReport(userId);

                // then
                assertThat(response).isNotNull();
                assertThat(response.totalSavedCount()).isEqualTo(1);
                assertThat(response.totalReadCount()).isEqualTo(1);
                // Balance (from savedList)
                assertThat(response.lightCount()).isEqualTo(1); // un1 is LIGHT
                assertThat(response.deepCount()).isEqualTo(0);
                assertThat(response.nowCount()).isEqualTo(1); // un1 is NOW
        }

        @Test
        @DisplayName("소비 현황 조회 - 최근 읽은 뉴스레터 매핑 확인")
        void getConsumption() {
                // given
                Long userId = 1L;
                List<UserNewsletter> recentReadList = new ArrayList<>();

                Newsletter newsletter = Newsletter.builder()
                                .title("Test Title")
                                .contentUrl("http://test.com")
                                .build();
                ReflectionTestUtils.setField(newsletter, "id", 100L);
                ReflectionTestUtils.setField(newsletter, "category", "IT/Science");

                UserNewsletter un = new UserNewsletter(User.builder().build(), newsletter, null, null, null);
                ReflectionTestUtils.setField(un, "lastViewedAt", LocalDateTime.now());

                recentReadList.add(un);

                when(userNewsletterRepository.findByUserIdAndCreatedAtBetween(eq(userId), any(), any()))
                                .thenReturn(Collections.emptyList());
                when(userNewsletterRepository.findByUserIdAndLastViewedAtBetweenAndIsReadTrue(eq(userId), any(), any()))
                                .thenReturn(Collections.emptyList());
                when(userNewsletterRepository.findByUserIdAndIsReadTrueOrderByLastViewedAtDesc(userId))
                                .thenReturn(recentReadList);

                // when
                ConsumptionResponse response = reportService.getConsumption(userId);

                // then
                assertThat(response.recentReadNewsletters()).hasSize(1);
                assertThat(response.recentReadNewsletters().get(0).title()).isEqualTo("Test Title");
                assertThat(response.recentReadNewsletters().get(0).categoryName()).isEqualTo("IT/Science");
        }

        @Test
        @DisplayName("관심사 갭 분석 - 토픽 ID 및 카운트 검증")
        void getGapAnalysis_ShouldReturnTopicIds() {
                // given
                Long userId = 1L;
                Long topicId = 10L;
                String topicName = "Economy";

                Topic topic = Topic.builder()
                                .name(topicName)
                                .build();
                ReflectionTestUtils.setField(topic, "id", topicId);

                // Saved UserNewsletter with Topic
                UserNewsletter savedUn = new UserNewsletter(null, null, null, topic, null);
                ReflectionTestUtils.setField(savedUn, "createdAt", LocalDateTime.now());

                // Read UserNewsletter with same Topic
                UserNewsletter readUn = new UserNewsletter(null, null, null, topic, null);
                ReflectionTestUtils.setField(readUn, "lastViewedAt", LocalDateTime.now());

                List<UserNewsletter> savedList = List.of(savedUn, savedUn); // 2 saved
                List<UserNewsletter> readList = List.of(readUn); // 1 read

                when(userNewsletterRepository.findByUserIdAndCreatedAtBetween(eq(userId), any(), any()))
                                .thenReturn(savedList);
                when(userNewsletterRepository.findByUserIdAndLastViewedAtBetweenAndIsReadTrue(eq(userId), any(), any()))
                                .thenReturn(readList);

                // when
                var response = reportService.getGapAnalysis(userId);

                // then
                assertThat(response.topics()).hasSize(1);
                assertThat(response.topics().get(0).id()).isEqualTo(topicId);
                assertThat(response.topics().get(0).name()).isEqualTo(topicName);
                assertThat(response.topics().get(0).savedCount()).isEqualTo(2);
                assertThat(response.topics().get(0).readCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("주간 리포트 - 빈 리스트 처리")
        void getWeeklyReport_WithEmptyLists() {
                // given
                Long userId = 1L;
                when(userNewsletterRepository.findByUserIdAndCreatedAtBetween(eq(userId), any(), any()))
                                .thenReturn(Collections.emptyList());
                when(userNewsletterRepository.findByUserIdAndLastViewedAtBetweenAndIsReadTrue(eq(userId), any(), any()))
                                .thenReturn(Collections.emptyList());

                // when
                WeeklyReportResponse response = reportService.getWeeklyReport(userId);

                // then
                assertThat(response).isNotNull();
                assertThat(response.totalSavedCount()).isEqualTo(0);
                assertThat(response.totalReadCount()).isEqualTo(0);
                assertThat(response.lightCount()).isEqualTo(0);
                assertThat(response.deepCount()).isEqualTo(0);
                assertThat(response.nowCount()).isEqualTo(0);
                assertThat(response.futureCount()).isEqualTo(0);
                assertThat(response.interestGaps()).isEmpty();
        }

        @Test
        @DisplayName("주간 리포트 - 모든 밸런스 타입 포함")
        void getWeeklyReport_WithAllBalanceTypes() {
                // given
                Long userId = 1L;
                List<UserNewsletter> savedList = new ArrayList<>();

                // Create newsletters with different balance types
                UserNewsletter light = new UserNewsletter(null, null, null, null, null);
                ReflectionTestUtils.setField(light, "depthType", DepthType.LIGHT);
                ReflectionTestUtils.setField(light, "perspectiveType", PerspectiveType.NOW);

                UserNewsletter deep = new UserNewsletter(null, null, null, null, null);
                ReflectionTestUtils.setField(deep, "depthType", DepthType.DEEP);
                ReflectionTestUtils.setField(deep, "perspectiveType", PerspectiveType.FUTURE);

                UserNewsletter light2 = new UserNewsletter(null, null, null, null, null);
                ReflectionTestUtils.setField(light2, "depthType", DepthType.LIGHT);
                ReflectionTestUtils.setField(light2, "perspectiveType", PerspectiveType.FUTURE);

                savedList.add(light);
                savedList.add(deep);
                savedList.add(light2);

                when(userNewsletterRepository.findByUserIdAndCreatedAtBetween(eq(userId), any(), any()))
                                .thenReturn(savedList);
                when(userNewsletterRepository.findByUserIdAndLastViewedAtBetweenAndIsReadTrue(eq(userId), any(), any()))
                                .thenReturn(Collections.emptyList());

                // when
                WeeklyReportResponse response = reportService.getWeeklyReport(userId);

                // then
                assertThat(response.totalSavedCount()).isEqualTo(3);
                assertThat(response.lightCount()).isEqualTo(2);
                assertThat(response.deepCount()).isEqualTo(1);
                assertThat(response.nowCount()).isEqualTo(1);
                assertThat(response.futureCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("관심사 갭 분석 - 다중 토픽 정렬 검증")
        void getGapAnalysis_WithMultipleTopics_ShouldSortByGap() {
                // given
                Long userId = 1L;

                // Create multiple topics
                com.archiveat.server.domain.explore.entity.Topic topic1 = com.archiveat.server.domain.explore.entity.Topic
                                .builder()
                                .name("Technology")
                                .build();
                ReflectionTestUtils.setField(topic1, "id", 1L);

                com.archiveat.server.domain.explore.entity.Topic topic2 = com.archiveat.server.domain.explore.entity.Topic
                                .builder()
                                .name("Economy")
                                .build();
                ReflectionTestUtils.setField(topic2, "id", 2L);

                com.archiveat.server.domain.explore.entity.Topic topic3 = com.archiveat.server.domain.explore.entity.Topic
                                .builder()
                                .name("Science")
                                .build();
                ReflectionTestUtils.setField(topic3, "id", 3L);

                // Create newsletters: topic1 has gap of 9 (10 saved - 1 read)
                List<UserNewsletter> savedList = new ArrayList<>();
                for (int i = 0; i < 10; i++) {
                        UserNewsletter un = new UserNewsletter(null, null, null, topic1, null);
                        savedList.add(un);
                }
                // topic2 has gap of 1 (3 saved - 2 read)
                for (int i = 0; i < 3; i++) {
                        UserNewsletter un = new UserNewsletter(null, null, null, topic2, null);
                        savedList.add(un);
                }
                // topic3 has gap of 5 (5 saved - 0 read)
                for (int i = 0; i < 5; i++) {
                        UserNewsletter un = new UserNewsletter(null, null, null, topic3, null);
                        savedList.add(un);
                }

                List<UserNewsletter> readList = new ArrayList<>();
                UserNewsletter readTopic1 = new UserNewsletter(null, null, null, topic1, null);
                readList.add(readTopic1);
                UserNewsletter readTopic2a = new UserNewsletter(null, null, null, topic2, null);
                UserNewsletter readTopic2b = new UserNewsletter(null, null, null, topic2, null);
                readList.add(readTopic2a);
                readList.add(readTopic2b);

                when(userNewsletterRepository.findByUserIdAndCreatedAtBetween(eq(userId), any(), any()))
                                .thenReturn(savedList);
                when(userNewsletterRepository.findByUserIdAndLastViewedAtBetweenAndIsReadTrue(eq(userId), any(), any()))
                                .thenReturn(readList);

                // when
                var response = reportService.getGapAnalysis(userId);

                // then
                assertThat(response.topics()).hasSize(3);
                // Should be sorted by gap: topic1 (gap=9), topic3 (gap=5), topic2 (gap=1)
                assertThat(response.topics().get(0).name()).isEqualTo("Technology");
                assertThat(response.topics().get(0).savedCount()).isEqualTo(10);
                assertThat(response.topics().get(0).readCount()).isEqualTo(1);

                assertThat(response.topics().get(1).name()).isEqualTo("Science");
                assertThat(response.topics().get(1).savedCount()).isEqualTo(5);
                assertThat(response.topics().get(1).readCount()).isEqualTo(0);

                assertThat(response.topics().get(2).name()).isEqualTo("Economy");
                assertThat(response.topics().get(2).savedCount()).isEqualTo(3);
                assertThat(response.topics().get(2).readCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("관심사 갭 분석 - 최대 4개 제한 검증")
        void getGapAnalysis_ShouldLimitToFourTopics() {
                // given
                Long userId = 1L;
                List<UserNewsletter> savedList = new ArrayList<>();
                List<UserNewsletter> readList = new ArrayList<>();

                // Create 5 topics but expect only top 4 by gap
                for (int i = 0; i < 5; i++) {
                        com.archiveat.server.domain.explore.entity.Topic topic = com.archiveat.server.domain.explore.entity.Topic
                                        .builder()
                                        .name("Topic" + (i + 1))
                                        .build();
                        ReflectionTestUtils.setField(topic, "id", (long) (i + 1));

                        // Create different gap sizes: 5, 4, 3, 2, 1
                        for (int j = 0; j < (5 - i); j++) {
                                UserNewsletter un = new UserNewsletter(null, null, null, topic, null);
                                savedList.add(un);
                        }
                }

                when(userNewsletterRepository.findByUserIdAndCreatedAtBetween(eq(userId), any(), any()))
                                .thenReturn(savedList);
                when(userNewsletterRepository.findByUserIdAndLastViewedAtBetweenAndIsReadTrue(eq(userId), any(), any()))
                                .thenReturn(readList);

                // when
                var response = reportService.getGapAnalysis(userId);

                // then
                assertThat(response.topics()).hasSize(4); // Limited to 4
        }

        @Test
        @DisplayName("소비 현황 - null Newsletter 처리")
        void getConsumption_WithNullNewsletter() {
                // given
                Long userId = 1L;
                UserNewsletter unWithNullNewsletter = new UserNewsletter(User.builder().build(), null, null, null,
                                null);
                ReflectionTestUtils.setField(unWithNullNewsletter, "lastViewedAt", LocalDateTime.now());

                List<UserNewsletter> recentReadList = List.of(unWithNullNewsletter);

                when(userNewsletterRepository.findByUserIdAndCreatedAtBetween(eq(userId), any(), any()))
                                .thenReturn(Collections.emptyList());
                when(userNewsletterRepository.findByUserIdAndLastViewedAtBetweenAndIsReadTrue(eq(userId), any(), any()))
                                .thenReturn(Collections.emptyList());
                when(userNewsletterRepository.findByUserIdAndIsReadTrueOrderByLastViewedAtDesc(userId))
                                .thenReturn(recentReadList);

                // when
                ConsumptionResponse response = reportService.getConsumption(userId);

                // then
                assertThat(response.recentReadNewsletters()).hasSize(1);
                assertThat(response.recentReadNewsletters().get(0).id()).isEqualTo(0L);
                assertThat(response.recentReadNewsletters().get(0).title()).isEqualTo("삭제된 뉴스레터");
                assertThat(response.recentReadNewsletters().get(0).categoryName()).isEqualTo("기타");
        }

        @Test
        @DisplayName("소비 현황 - null category 처리")
        void getConsumption_WithNullCategory() {
                // given
                Long userId = 1L;
                Newsletter newsletter = Newsletter.builder()
                                .title("Test Newsletter")
                                .contentUrl("http://test.com")
                                .build();
                ReflectionTestUtils.setField(newsletter, "id", 100L);
                // category is null by default

                UserNewsletter un = new UserNewsletter(User.builder().build(), newsletter, null, null, null);
                ReflectionTestUtils.setField(un, "lastViewedAt", LocalDateTime.now());

                List<UserNewsletter> recentReadList = List.of(un);

                when(userNewsletterRepository.findByUserIdAndCreatedAtBetween(eq(userId), any(), any()))
                                .thenReturn(Collections.emptyList());
                when(userNewsletterRepository.findByUserIdAndLastViewedAtBetweenAndIsReadTrue(eq(userId), any(), any()))
                                .thenReturn(Collections.emptyList());
                when(userNewsletterRepository.findByUserIdAndIsReadTrueOrderByLastViewedAtDesc(userId))
                                .thenReturn(recentReadList);

                // when
                ConsumptionResponse response = reportService.getConsumption(userId);

                // then
                assertThat(response.recentReadNewsletters()).hasSize(1);
                assertThat(response.recentReadNewsletters().get(0).categoryName()).isEqualTo("기타");
        }

        @Test
        @DisplayName("관심사 갭 - null Topic 처리")
        void getGapAnalysis_WithNullTopic() {
                // given
                Long userId = 1L;
                UserNewsletter unWithNullTopic = new UserNewsletter(null, null, null, null, null);

                List<UserNewsletter> savedList = List.of(unWithNullTopic);
                List<UserNewsletter> readList = Collections.emptyList();

                when(userNewsletterRepository.findByUserIdAndCreatedAtBetween(eq(userId), any(), any()))
                                .thenReturn(savedList);
                when(userNewsletterRepository.findByUserIdAndLastViewedAtBetweenAndIsReadTrue(eq(userId), any(), any()))
                                .thenReturn(readList);

                // when
                var response = reportService.getGapAnalysis(userId);

                // then
                assertThat(response.topics()).isEmpty(); // null topics are filtered out
        }

        @Test
        @DisplayName("주간 리포트 - 주차 라벨 포함 확인")
        void getWeeklyReport_ShouldIncludeWeekLabel() {
                // given
                Long userId = 1L;
                when(userNewsletterRepository.findByUserIdAndCreatedAtBetween(eq(userId), any(), any()))
                                .thenReturn(Collections.emptyList());
                when(userNewsletterRepository.findByUserIdAndLastViewedAtBetweenAndIsReadTrue(eq(userId), any(), any()))
                                .thenReturn(Collections.emptyList());

                // when
                WeeklyReportResponse response = reportService.getWeeklyReport(userId);

                // then
                assertThat(response.weekLabel()).isNotNull();
                assertThat(response.weekLabel()).matches("\\d+월 .*주"); // Pattern: "N월 *주"
        }

        @Test
        @DisplayName("주간 리포트 - AI 코멘트 하드코딩 확인")
        void getWeeklyReport_ShouldIncludeHardcodedAiComment() {
                // given
                Long userId = 1L;
                when(userNewsletterRepository.findByUserIdAndCreatedAtBetween(eq(userId), any(), any()))
                                .thenReturn(Collections.emptyList());
                when(userNewsletterRepository.findByUserIdAndLastViewedAtBetweenAndIsReadTrue(eq(userId), any(), any()))
                                .thenReturn(Collections.emptyList());

                // when
                WeeklyReportResponse response = reportService.getWeeklyReport(userId);

                // then
                assertThat(response.aiComment()).isNotNull();
                assertThat(response.aiComment()).isEqualTo(
                                "편식 없는 지식 섭취가 필요해요! IT 트렌드는 잘 따라가고 있지만, 경제 분야는 놓치고 있어요.");
        }

        @Test
        @DisplayName("소비 밸런스 - Light가 Deep보다 많은 경우")
        void getBalance_WhenLightMoreThanDeep() {
                // given
                Long userId = 1L;
                List<UserNewsletter> readList = new ArrayList<>();

                // Create mostly light content
                for (int i = 0; i < 5; i++) {
                        UserNewsletter light = new UserNewsletter(null, null, null, null, null);
                        ReflectionTestUtils.setField(light, "depthType", DepthType.LIGHT);
                        ReflectionTestUtils.setField(light, "perspectiveType", PerspectiveType.NOW);
                        readList.add(light);
                }

                UserNewsletter deep = new UserNewsletter(null, null, null, null, null);
                ReflectionTestUtils.setField(deep, "depthType", DepthType.DEEP);
                ReflectionTestUtils.setField(deep, "perspectiveType", PerspectiveType.FUTURE);
                readList.add(deep);

                when(userNewsletterRepository.findByUserIdAndLastViewedAtBetweenAndIsReadTrue(eq(userId), any(), any()))
                                .thenReturn(readList);

                // when
                var response = reportService.getBalance(userId);

                // then
                assertThat(response).isNotNull();
                assertThat(response.lightCount()).isEqualTo(5);
                assertThat(response.deepCount()).isEqualTo(1);
                assertThat(response.patternTitle()).isEqualTo("핵심을 빠르게 파악하는 당신");
                assertThat(response.patternDescription()).isEqualTo("10분 미만의 가볍고 빠른 콘텐츠를 선호하시네요!");
                assertThat(response.patternQuote()).isEqualTo("빠르고 효율적인 학습이 강점입니다.");
        }

        @Test
        @DisplayName("소비 밸런스 - Deep이 Light보다 많은 경우")
        void getBalance_WhenDeepMoreThanLight() {
                // given
                Long userId = 1L;
                List<UserNewsletter> readList = new ArrayList<>();

                // Create mostly deep content
                for (int i = 0; i < 5; i++) {
                        UserNewsletter deep = new UserNewsletter(null, null, null, null, null);
                        ReflectionTestUtils.setField(deep, "depthType", DepthType.DEEP);
                        ReflectionTestUtils.setField(deep, "perspectiveType", PerspectiveType.FUTURE);
                        readList.add(deep);
                }

                UserNewsletter light = new UserNewsletter(null, null, null, null, null);
                ReflectionTestUtils.setField(light, "depthType", DepthType.LIGHT);
                ReflectionTestUtils.setField(light, "perspectiveType", PerspectiveType.NOW);
                readList.add(light);

                when(userNewsletterRepository.findByUserIdAndLastViewedAtBetweenAndIsReadTrue(eq(userId), any(), any()))
                                .thenReturn(readList);

                // when
                var response = reportService.getBalance(userId);

                // then
                assertThat(response).isNotNull();
                assertThat(response.lightCount()).isEqualTo(1);
                assertThat(response.deepCount()).isEqualTo(5);
                assertThat(response.patternTitle()).isEqualTo("깊이 있는 통찰을 추구하는 당신");
                assertThat(response.patternDescription()).isEqualTo("심도 있는 긴 콘텐츠를 즐기시는군요!");
                assertThat(response.patternQuote()).isEqualTo("깊이 있는 사고가 당신의 무기입니다.");
        }

        @Test
        @DisplayName("소비 밸런스 - 동일한 Light/Deep 비율")
        void getBalance_WhenEqualLightAndDeep() {
                // given
                Long userId = 1L;
                List<UserNewsletter> readList = new ArrayList<>();

                for (int i = 0; i < 3; i++) {
                        UserNewsletter light = new UserNewsletter(null, null, null, null, null);
                        ReflectionTestUtils.setField(light, "depthType", DepthType.LIGHT);
                        ReflectionTestUtils.setField(light, "perspectiveType", PerspectiveType.NOW);
                        readList.add(light);

                        UserNewsletter deep = new UserNewsletter(null, null, null, null, null);
                        ReflectionTestUtils.setField(deep, "depthType", DepthType.DEEP);
                        ReflectionTestUtils.setField(deep, "perspectiveType", PerspectiveType.FUTURE);
                        readList.add(deep);
                }

                when(userNewsletterRepository.findByUserIdAndLastViewedAtBetweenAndIsReadTrue(eq(userId), any(), any()))
                                .thenReturn(readList);

                // when
                var response = reportService.getBalance(userId);

                // then
                assertThat(response.lightCount()).isEqualTo(3);
                assertThat(response.deepCount()).isEqualTo(3);
                assertThat(response.patternTitle()).isEqualTo("깊이 있는 통찰을 추구하는 당신");
        }

        @Test
        @DisplayName("소비 밸런스 - 빈 리스트")
        void getBalance_WithEmptyList() {
                // given
                Long userId = 1L;
                when(userNewsletterRepository.findByUserIdAndLastViewedAtBetweenAndIsReadTrue(eq(userId), any(), any()))
                                .thenReturn(Collections.emptyList());

                // when
                var response = reportService.getBalance(userId);

                // then
                assertThat(response).isNotNull();
                assertThat(response.lightCount()).isEqualTo(0);
                assertThat(response.deepCount()).isEqualTo(0);
                assertThat(response.nowCount()).isEqualTo(0);
                assertThat(response.futureCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("소비 밸런스 - Now/Future 카운트 검증")
        void getBalance_NowAndFutureCounts() {
                // given
                Long userId = 1L;
                List<UserNewsletter> readList = new ArrayList<>();

                for (int i = 0; i < 4; i++) {
                        UserNewsletter now = new UserNewsletter(null, null, null, null, null);
                        ReflectionTestUtils.setField(now, "depthType", DepthType.LIGHT);
                        ReflectionTestUtils.setField(now, "perspectiveType", PerspectiveType.NOW);
                        readList.add(now);
                }

                for (int i = 0; i < 2; i++) {
                        UserNewsletter future = new UserNewsletter(null, null, null, null, null);
                        ReflectionTestUtils.setField(future, "depthType", DepthType.DEEP);
                        ReflectionTestUtils.setField(future, "perspectiveType", PerspectiveType.FUTURE);
                        readList.add(future);
                }

                when(userNewsletterRepository.findByUserIdAndLastViewedAtBetweenAndIsReadTrue(eq(userId), any(), any()))
                                .thenReturn(readList);

                // when
                var response = reportService.getBalance(userId);

                // then
                assertThat(response.nowCount()).isEqualTo(4);
                assertThat(response.futureCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("소비 현황 - 저장/읽음 카운트와 최근 읽은 리스트")
        void getConsumption_WithMultipleNewsletters() {
                // given
                Long userId = 1L;
                List<UserNewsletter> savedList = new ArrayList<>();
                List<UserNewsletter> readList = new ArrayList<>();

                // Create saved newsletters
                for (int i = 0; i < 3; i++) {
                        savedList.add(new UserNewsletter(null, null, null, null, null));
                }

                // Create read newsletters
                for (int i = 0; i < 2; i++) {
                        readList.add(new UserNewsletter(null, null, null, null, null));
                }

                // Create recent read list
                List<UserNewsletter> recentReadList = new ArrayList<>();
                for (int i = 0; i < 5; i++) {
                        Newsletter newsletter = Newsletter.builder()
                                        .title("Newsletter " + (i + 1))
                                        .contentUrl("http://test" + i + ".com")
                                        .build();
                        ReflectionTestUtils.setField(newsletter, "id", (long) (i + 1));
                        ReflectionTestUtils.setField(newsletter, "category", "Category" + i);

                        UserNewsletter un = new UserNewsletter(User.builder().build(), newsletter, null, null, null);
                        ReflectionTestUtils.setField(un, "lastViewedAt", LocalDateTime.now().minusDays(i));
                        recentReadList.add(un);
                }

                when(userNewsletterRepository.findByUserIdAndCreatedAtBetween(eq(userId), any(), any()))
                                .thenReturn(savedList);
                when(userNewsletterRepository.findByUserIdAndLastViewedAtBetweenAndIsReadTrue(eq(userId), any(), any()))
                                .thenReturn(readList);
                when(userNewsletterRepository.findByUserIdAndIsReadTrueOrderByLastViewedAtDesc(userId))
                                .thenReturn(recentReadList);

                // when
                ConsumptionResponse response = reportService.getConsumption(userId);

                // then
                assertThat(response.totalSavedCount()).isEqualTo(3);
                assertThat(response.totalReadCount()).isEqualTo(2);
                assertThat(response.recentReadNewsletters()).hasSize(5);
                assertThat(response.recentReadNewsletters().get(0).title()).isEqualTo("Newsletter 1");
        }
}
