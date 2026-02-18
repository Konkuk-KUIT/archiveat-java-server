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

                // [Insight] 생성자 대신 정의된 @Builder를 사용하여 가독성 확보
                UserNewsletter un1 = UserNewsletter.builder()
                                .depthType(DepthType.LIGHT)
                                .perspectiveType(PerspectiveType.NOW)
                                .lastViewedAt(LocalDateTime.now())
                                .build();

                UserNewsletter un2 = UserNewsletter.builder()
                                .depthType(DepthType.DEEP)
                                .perspectiveType(PerspectiveType.FUTURE)
                                .lastViewedAt(LocalDateTime.now())
                                .build();

                when(userNewsletterRepository.findByUserIdAndCreatedAtBetween(eq(userId), any(), any()))
                                .thenReturn(List.of(un1));
                // Balance calculation now uses read newsletters, so un1 (Light) must be in this
                // list to pass assertThat(lightCount).isEqualTo(1)
                when(userNewsletterRepository.findByUserIdAndLastViewedAtBetweenAndIsReadTrue(eq(userId), any(), any()))
                                .thenReturn(List.of(un1, un2));

                // when
                WeeklyReportResponse response = reportService.getWeeklyReport(userId);

                // then
                assertThat(response).isNotNull();
                assertThat(response.totalSavedCount()).isEqualTo(1);
                assertThat(response.totalReadCount()).isEqualTo(2);
                assertThat(response.lightCount()).isEqualTo(1); // un1 기준
                assertThat(response.nowCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("소비 현황 조회 - 최근 읽은 뉴스레터 매핑 확인")
        void getConsumption() {
                // given
                Long userId = 1L;

                Newsletter newsletter = Newsletter.builder()
                                .title("Test Title")
                                .contentUrl("http://test.com")
                                .build();

                // [Reason] Newsletter 빌더에 없는 필드(id, category)만 리플렉션 사용
                ReflectionTestUtils.setField(newsletter, "id", 100L);
                ReflectionTestUtils.setField(newsletter, "category", "IT/Science");

                UserNewsletter un = UserNewsletter.builder()
                                .newsletter(newsletter)
                                .lastViewedAt(LocalDateTime.now())
                                .build();

                when(userNewsletterRepository.findByUserIdAndCreatedAtBetween(eq(userId), any(), any()))
                                .thenReturn(Collections.emptyList());
                when(userNewsletterRepository.findByUserIdAndLastViewedAtBetweenAndIsReadTrue(eq(userId), any(), any()))
                                .thenReturn(Collections.emptyList());
                when(userNewsletterRepository.findByUserIdAndIsReadTrueOrderByLastViewedAtDesc(userId))
                                .thenReturn(List.of(un));

                // when
                ConsumptionResponse response = reportService.getConsumption(userId);

                // then
                assertThat(response.recentReadNewsletters()).hasSize(1);
                assertThat(response.recentReadNewsletters().getFirst().title()).isEqualTo("Test Title");
                assertThat(response.recentReadNewsletters().getFirst().categoryName()).isEqualTo("IT/Science");
        }

        @Test
        @DisplayName("관심사 갭 분석 - 토픽 ID 및 카운트 검증")
        void getGapAnalysis_ShouldReturnTopicIds() {
                // given
                Long userId = 1L;
                Long topicId = 10L;

                Topic topic = Topic.builder().name("Economy").build();
                ReflectionTestUtils.setField(topic, "id", topicId); // ID는 수동 주입

                UserNewsletter savedUn = UserNewsletter.builder()
                                .topic(topic)
                                .lastViewedAt(LocalDateTime.now())
                                .build();

                UserNewsletter readUn = UserNewsletter.builder()
                                .topic(topic)
                                .lastViewedAt(LocalDateTime.now())
                                .build();

                when(userNewsletterRepository.findByUserIdAndCreatedAtBetween(eq(userId), any(), any()))
                                .thenReturn(List.of(savedUn, savedUn)); // 2개 저장
                when(userNewsletterRepository.findByUserIdAndLastViewedAtBetweenAndIsReadTrue(eq(userId), any(), any()))
                                .thenReturn(List.of(readUn)); // 1개 읽음

                // when
                var response = reportService.getGapAnalysis(userId);

                // then
                assertThat(response.topics()).hasSize(1);
                assertThat(response.topics().getFirst().id()).isEqualTo(topicId);
                assertThat(response.topics().getFirst().savedCount()).isEqualTo(2);
                assertThat(response.topics().getFirst().readCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("소비 밸런스 - 패턴 메시지 및 카운트 검증")
        void getBalance_PatternCheck() {
                // given
                Long userId = 1L;

                // Light 2개, Deep 1개 생성 (Light 선호 패턴)
                UserNewsletter light = UserNewsletter.builder().depthType(DepthType.LIGHT).build();
                UserNewsletter deep = UserNewsletter.builder().depthType(DepthType.DEEP).build();

                when(userNewsletterRepository.findByUserIdAndLastViewedAtBetweenAndIsReadTrue(eq(userId), any(), any()))
                                .thenReturn(List.of(light, light, deep));

                // when
                var response = reportService.getBalance(userId);

                // then
                assertThat(response.lightCount()).isEqualTo(2);
                assertThat(response.deepCount()).isEqualTo(1);
                assertThat(response.patternTitle()).isEqualTo("핵심을 빠르게 파악하는 당신");
        }

        @Test
        @DisplayName("주간 리포트 - 빈 데이터 시 0 반환 확인")
        void getWeeklyReport_Empty() {
                // given
                when(userNewsletterRepository.findByUserIdAndCreatedAtBetween(any(), any(), any()))
                                .thenReturn(Collections.emptyList());
                when(userNewsletterRepository.findByUserIdAndLastViewedAtBetweenAndIsReadTrue(any(), any(), any()))
                                .thenReturn(Collections.emptyList());

                // when
                WeeklyReportResponse response = reportService.getWeeklyReport(1L);

                // then
                assertThat(response.totalSavedCount()).isEqualTo(0);
                assertThat(response.interestGaps()).isEmpty();
        }
}