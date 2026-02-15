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
}
