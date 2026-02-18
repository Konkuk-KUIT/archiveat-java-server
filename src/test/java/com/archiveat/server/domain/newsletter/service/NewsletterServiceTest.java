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
import static org.mockito.Mockito.verify;

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

        Newsletter newsletter = Newsletter.builder()
                .title("Newsletter Title")
                .contentUrl("http://example.com")
                .llmStatus(LlmStatus.DONE)
                .build();
        ReflectionTestUtils.setField(newsletter, "id", 1L);
        ReflectionTestUtils.setField(newsletter, "newsletterSummary", summaryJson);
        ReflectionTestUtils.setField(newsletter, "thumbnailUrl", "http://thumb.url");

        UserNewsletter userNewsletter = UserNewsletter.builder()
                .newsletter(newsletter)
                .memo("Memo")
                .depthType(DepthType.DEEP)
                .perspectiveType(PerspectiveType.NOW)
                .build();
        ReflectionTestUtils.setField(userNewsletter, "id", userNewsletterId);
        ReflectionTestUtils.setField(userNewsletter, "isRead", false);

        given(userNewsletterRepository.findByIdAndUser_Id(userNewsletterId, userId))
                .willReturn(Optional.of(userNewsletter));

        // when
        SimpleViewNewsletterResponse response = newsletterService.simpleViewUserNewsletter(userId, userNewsletterId, true);

        // then
        assertThat(response.newsletterSimpleSummary()).hasSize(1);
        assertThat(response.newsletterSimpleSummary().getFirst().title()).isEqualTo("Summary 1");
        assertThat(response.isRead()).isTrue();
    }

    @Test
    @DisplayName("simpleViewUserNewsletter returns empty list when summary is empty")
    void simpleViewUserNewsletter_ReturnsEmptyList_WhenSummaryIsEmpty() throws Exception {
        // given
        Long userId = 1L;
        Long userNewsletterId = 100L;

        Newsletter newsletter = Newsletter.builder()
                .title("Newsletter Title")
                .contentUrl("http://example.com")
                .build();
        ReflectionTestUtils.setField(newsletter, "id", 1L);
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
        assertThat(response.isRead()).isTrue();
    }

    @Test
    @DisplayName("viewUserNewsletter should return personalized classification if available")
    void viewUserNewsletter_ShouldReturnPersonalizedClassification() {
        // given
        Long userId = 1L;
        Long userNewsletterId = 10L;

        Category userCategory = Category.builder().name("PersonalizedCategory").build();
        ReflectionTestUtils.setField(userCategory, "id", 1L);
        Topic userTopic = Topic.builder().name("PersonalizedTopic").build();
        ReflectionTestUtils.setField(userTopic, "id", 10L);

        Newsletter newsletter = Newsletter.builder()
                .contentUrl("http://url")
                .build();
        ReflectionTestUtils.setField(newsletter, "id", 1L);
        ReflectionTestUtils.setField(newsletter, "newsletterSummary", "[]");

        UserNewsletter userNewsletter = UserNewsletter.builder()
                .newsletter(newsletter)
                .category(userCategory)
                .topic(userTopic)
                .build();
        ReflectionTestUtils.setField(userNewsletter, "id", userNewsletterId);
        ReflectionTestUtils.setField(userNewsletter, "modifiedAt", LocalDateTime.now());
        ReflectionTestUtils.setField(userNewsletter, "isRead", false);

        given(userNewsletterRepository.findByIdAndUser_Id(userNewsletterId, userId))
                .willReturn(Optional.of(userNewsletter));

        // when
        ViewNewsletterResponse response = newsletterService.viewUserNewsletter(userId, userNewsletterId, false);

        // then
        assertThat(response.categoryName()).isEqualTo("PersonalizedCategory");
        assertThat(response.topicName()).isEqualTo("PersonalizedTopic");
        assertThat(response.isRead()).isFalse();
    }

    @Test
    @DisplayName("viewUserNewsletter should fallback to original classification when user classification is missing")
    void viewUserNewsletter_ShouldFallbackToOriginalClassification() {
        // given
        Long userId = 1L;
        Long userNewsletterId = 11L;

        Newsletter newsletter = Newsletter.builder()
                .contentUrl("http://url")
                .build();
        ReflectionTestUtils.setField(newsletter, "id", 1L);
        ReflectionTestUtils.setField(newsletter, "category", "OriginalCategory");
        ReflectionTestUtils.setField(newsletter, "topic", "OriginalTopic");
        ReflectionTestUtils.setField(newsletter, "newsletterSummary", "[]");

        UserNewsletter userNewsletter = UserNewsletter.builder()
                .newsletter(newsletter)
                .category(null)
                .topic(null)
                .build();
        ReflectionTestUtils.setField(userNewsletter, "id", userNewsletterId);
        ReflectionTestUtils.setField(userNewsletter, "modifiedAt", LocalDateTime.now());
        ReflectionTestUtils.setField(userNewsletter, "isRead", false);

        given(userNewsletterRepository.findByIdAndUser_Id(userNewsletterId, userId))
                .willReturn(Optional.of(userNewsletter));

        // when
        ViewNewsletterResponse response = newsletterService.viewUserNewsletter(userId, userNewsletterId, false);

        // then
        assertThat(response.categoryName()).isEqualTo("OriginalCategory");
        assertThat(response.topicName()).isEqualTo("OriginalTopic");
        assertThat(response.isRead()).isFalse();
    }

    @Test
    @DisplayName("updateIsRead should update status and save userNewsletter")
    void updateIsRead_ShouldUpdateStatusAndSave() {
        // given
        Long userId = 1L;
        Long userNewsletterId = 100L;

        Newsletter newsletter = Newsletter.builder()
                .contentUrl("http://example.com")
                .build();

        UserNewsletter userNewsletter = UserNewsletter.builder()
                .newsletter(newsletter)
                .build();
        ReflectionTestUtils.setField(userNewsletter, "id", userNewsletterId);
        ReflectionTestUtils.setField(userNewsletter, "isRead", false);

        given(userNewsletterRepository.findByIdAndUser_Id(userNewsletterId, userId))
                .willReturn(Optional.of(userNewsletter));

        // when
        newsletterService.updateIsRead(userId, userNewsletterId);

        // then
        assertThat(userNewsletter.isRead()).isTrue();
        verify(userNewsletterRepository).save(userNewsletter);
    }
}