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
import static org.mockito.Mockito.verifyNoMoreInteractions;

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
        ReflectionTestUtils.setField(userNewsletter, "isRead", true); // 이미 읽음 상태인 경우를 가정 (사전조건)

        given(userNewsletterRepository.findByIdAndUser_Id(userNewsletterId, userId))
                .willReturn(Optional.of(userNewsletter));

        // when - 팀원이 변경한 시그니처 반영 (boolean 파라미터 제거)
        SimpleViewNewsletterResponse response = newsletterService.simpleViewUserNewsletter(userId, userNewsletterId);

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
        // 초기값은 false이지만 서비스 호출 후에는 true가 되어야 합니다.
        ReflectionTestUtils.setField(userNewsletter, "isRead", false);

        given(userNewsletterRepository.findByIdAndUser_Id(userNewsletterId, userId))
                .willReturn(Optional.of(userNewsletter));

        // when
        SimpleViewNewsletterResponse response = newsletterService.simpleViewUserNewsletter(userId, userNewsletterId);

        // then
        assertThat(response.newsletterSimpleSummary()).isEmpty();
        assertThat(response.isRead()).isTrue();

        // 상태가 변경되었으므로 save() 메서드가 호출되었는지 검증합니다.
        verify(userNewsletterRepository).save(userNewsletter);
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

        // when - 시그니처 반영 (boolean 파라미터 제거)
        ViewNewsletterResponse response = newsletterService.viewUserNewsletter(userId, userNewsletterId);

        // then
        assertThat(response.categoryName()).isEqualTo("PersonalizedCategory");
        assertThat(response.topicName()).isEqualTo("PersonalizedTopic");
        assertThat(response.isRead()).isFalse();
        verifyNoMoreInteractions(userNewsletterRepository);
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
                .build();
        ReflectionTestUtils.setField(userNewsletter, "id", userNewsletterId);
        ReflectionTestUtils.setField(userNewsletter, "modifiedAt", LocalDateTime.now());
        ReflectionTestUtils.setField(userNewsletter, "isRead", false);

        given(userNewsletterRepository.findByIdAndUser_Id(userNewsletterId, userId))
                .willReturn(Optional.of(userNewsletter));

        // when - 시그니처 반영
        ViewNewsletterResponse response = newsletterService.viewUserNewsletter(userId, userNewsletterId);

        // then
        assertThat(response.categoryName()).isEqualTo("OriginalCategory");
        assertThat(response.topicName()).isEqualTo("OriginalTopic");
        assertThat(response.isRead()).isFalse();
        verifyNoMoreInteractions(userNewsletterRepository);
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

    @Test
    @DisplayName("이미 읽음 상태일 때 updateIsRead 호출 시 true 상태를 유지한다 (멱등성 보장)")
    void updateIsRead_ShouldMaintainStatusAndSave_WhenAlreadyRead() {
        // given
        Long userId = 1L;
        Long userNewsletterId = 101L;

        Newsletter newsletter = Newsletter.builder()
                .contentUrl("http://example.com")
                .build();

        UserNewsletter userNewsletter = UserNewsletter.builder()
                .newsletter(newsletter)
                .build();
        ReflectionTestUtils.setField(userNewsletter, "id", userNewsletterId);
        ReflectionTestUtils.setField(userNewsletter, "isRead", true); // [Reason] 이미 true인 상태를 가정합니다.

        given(userNewsletterRepository.findByIdAndUser_Id(userNewsletterId, userId))
                .willReturn(Optional.of(userNewsletter));

        // when
        newsletterService.updateIsRead(userId, userNewsletterId);

        // then
        assertThat(userNewsletter.isRead()).isTrue(); // [Reason] 다시 false로 변하지 않고 true가 유지되는지 확인합니다.
        verify(userNewsletterRepository).save(userNewsletter); // 상태 변화가 없더라도 save는 호출되어 시간 등이 갱신될 수 있음을 확인합니다.
    }
}