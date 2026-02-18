package com.archiveat.server.domain.newsletter.service;

import com.archiveat.server.domain.explore.entity.Category;
import com.archiveat.server.domain.explore.entity.Topic;
import com.archiveat.server.domain.newsletter.dto.response.SimpleViewNewsletterResponse;
import com.archiveat.server.domain.newsletter.dto.response.ViewNewsletterResponse;
import com.archiveat.server.domain.newsletter.entity.Newsletter;
import com.archiveat.server.domain.newsletter.entity.UserNewsletter;
import com.archiveat.server.domain.newsletter.repository.UserNewsletterRepository;
import com.archiveat.server.global.common.constant.DepthType;
import com.archiveat.server.global.common.constant.LlmStatus;
import com.archiveat.server.global.common.constant.PerspectiveType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class NewsletterServiceTest {

    @InjectMocks
    private NewsletterService newsletterService;

    @Mock
    private UserNewsletterRepository userNewsletterRepository;

    @Spy
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("simpleViewUserNewsletter returns the first summary block")
    void simpleViewUserNewsletter_ReturnsFirstSummaryBlock() throws Exception {
        // given
        Long userId = 1L;
        Long userNewsletterId = 100L;
        String summaryJson = "[{\"title\":\"Summary 1\",\"content\":\"Content 1\"}, {\"title\":\"Summary 2\",\"content\":\"Content 2\"}]";

        // 1. Newsletter 빌더를 통한 실제 객체 생성
        Newsletter newsletter = Newsletter.builder()
                .id(1L)
                .title("Newsletter Title")
                .contentUrl("http://example.com")
                .llmStatus(LlmStatus.DONE)
                .build();
        // 빌더에 없는 필드는 Reflection으로 주입하여 NPE 방지 및 데이터 설정
        ReflectionTestUtils.setField(newsletter, "newsletterSummary", summaryJson);
        ReflectionTestUtils.setField(newsletter, "thumbnailUrl", "http://thumb.url");

        // 2. UserNewsletter 빌더를 통한 실제 객체 생성
        UserNewsletter userNewsletter = UserNewsletter.builder()
                .newsletter(newsletter)
                .memo("Memo")
                .depthType(DepthType.DEEP)
                .perspectiveType(PerspectiveType.NOW)
                .build();
        ReflectionTestUtils.setField(userNewsletter, "id", userNewsletterId);

        given(userNewsletterRepository.findByIdAndUser_Id(userNewsletterId, userId))
                .willReturn(Optional.of(userNewsletter));

        // when
        SimpleViewNewsletterResponse response = newsletterService.simpleViewUserNewsletter(userId, userNewsletterId, true);

        // then
        assertThat(response.newsletterSimpleSummary()).hasSize(1);
        assertThat(response.newsletterSimpleSummary().get(0).title()).isEqualTo("Summary 1");
        assertThat(response.newsletterSimpleSummary().get(0).content()).isEqualTo("Content 1");
    }

    @Test
    @DisplayName("simpleViewUserNewsletter returns empty list when summary is empty")
    void simpleViewUserNewsletter_ReturnsEmptyList_WhenSummaryIsEmpty() throws Exception {
        // given
        Long userId = 1L;
        Long userNewsletterId = 100L;

        Newsletter newsletter = Newsletter.builder()
                .id(1L)
                .title("Newsletter Title")
                .contentUrl("http://example.com")
                .build();
        ReflectionTestUtils.setField(newsletter, "newsletterSummary", "[]");

        UserNewsletter userNewsletter = UserNewsletter.builder()
                .newsletter(newsletter)
                .depthType(DepthType.DEEP)
                .perspectiveType(PerspectiveType.NOW)
                .build();
        ReflectionTestUtils.setField(userNewsletter, "id", userNewsletterId);

        given(userNewsletterRepository.findByIdAndUser_Id(userNewsletterId, userId))
                .willReturn(Optional.of(userNewsletter));

        // when
        SimpleViewNewsletterResponse response = newsletterService.simpleViewUserNewsletter(userId, userNewsletterId, true);

        // then
        assertThat(response.newsletterSimpleSummary()).isEmpty();
    }

    @Test
    @DisplayName("viewUserNewsletter should return personalized classification if available")
    void viewUserNewsletter_ShouldReturnPersonalizedClassification() {
        // given
        Long userId = 1L;
        Long userNewsletterId = 10L;

        // 실제 Category와 Topic 객체 생성
        Category userCategory = Category.builder().id(1L).name("PersonalizedCategory").build();
        Topic userTopic = Topic.builder().id(10L).name("PersonalizedTopic").build();

        Newsletter newsletter = Newsletter.builder()
                .id(1L)
                .contentUrl("http://url")
                .build();
        ReflectionTestUtils.setField(newsletter, "newsletterSummary", "[]");

        // 유저가 개인화한 정보를 담은 UserNewsletter 객체 생성
        UserNewsletter userNewsletter = UserNewsletter.builder()
                .newsletter(newsletter)
                .category(userCategory)
                .topic(userTopic)
                .build();
        ReflectionTestUtils.setField(userNewsletter, "id", userNewsletterId);
        ReflectionTestUtils.setField(userNewsletter, "modifiedAt", LocalDateTime.now()); // NPE 방어

        given(userNewsletterRepository.findByIdAndUser_Id(userNewsletterId, userId))
                .willReturn(Optional.of(userNewsletter));

        // when
        ViewNewsletterResponse response = newsletterService.viewUserNewsletter(userId, userNewsletterId, false);

        // then
        assertThat(response.categoryName()).isEqualTo("PersonalizedCategory");
        assertThat(response.topicName()).isEqualTo("PersonalizedTopic");
    }

    @Test
    @DisplayName("viewUserNewsletter should fallback to original classification when user classification is missing")
    void viewUserNewsletter_ShouldFallbackToOriginalClassification() {
        // given
        Long userId = 1L;
        Long userNewsletterId = 11L;

        Newsletter newsletter = Newsletter.builder()
                .id(1L)
                .contentUrl("http://url")
                .build();
        // Newsletter 엔티티가 가진 원본 메타데이터 (빌더에 없으므로 Reflection 사용)
        ReflectionTestUtils.setField(newsletter, "category", "OriginalCategory");
        ReflectionTestUtils.setField(newsletter, "topic", "OriginalTopic");
        ReflectionTestUtils.setField(newsletter, "newsletterSummary", "[]");

        // 카테고리와 토픽이 null인 상태 (폴백 상황 시뮬레이션)
        UserNewsletter userNewsletter = UserNewsletter.builder()
                .newsletter(newsletter)
                .category(null)
                .topic(null)
                .build();
        ReflectionTestUtils.setField(userNewsletter, "id", userNewsletterId);
        ReflectionTestUtils.setField(userNewsletter, "modifiedAt", LocalDateTime.now());

        given(userNewsletterRepository.findByIdAndUser_Id(userNewsletterId, userId))
                .willReturn(Optional.of(userNewsletter));

        // when
        ViewNewsletterResponse response = newsletterService.viewUserNewsletter(userId, userNewsletterId, false);

        // then
        assertThat(response.categoryName()).isEqualTo("OriginalCategory");
        assertThat(response.topicName()).isEqualTo("OriginalTopic");
    }
}