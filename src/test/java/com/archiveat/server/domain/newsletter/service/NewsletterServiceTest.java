package com.archiveat.server.domain.newsletter.service;

import com.archiveat.server.domain.explore.entity.Category;
import com.archiveat.server.domain.explore.entity.Topic;
import com.archiveat.server.domain.newsletter.dto.response.GenerateNewsletterResponse;
import com.archiveat.server.domain.newsletter.dto.response.SimpleViewNewsletterResponse;
import com.archiveat.server.domain.newsletter.dto.response.ViewNewsletterResponse;
import com.archiveat.server.domain.newsletter.entity.Domain;
import com.archiveat.server.domain.newsletter.entity.Newsletter;
import com.archiveat.server.domain.newsletter.entity.UserNewsletter;
import com.archiveat.server.domain.newsletter.event.NewsletterProcessRequestedEvent;
import com.archiveat.server.domain.newsletter.repository.DomainRepository;
import com.archiveat.server.domain.newsletter.repository.UserNewsletterRepository;
import com.archiveat.server.domain.user.entity.User;
import com.archiveat.server.domain.user.repository.UserRepository;
import com.archiveat.server.global.common.constant.DepthType;
import com.archiveat.server.global.common.constant.LlmStatus;
import com.archiveat.server.global.common.constant.PerspectiveType;
import com.archiveat.server.global.util.UrlRedirectResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class NewsletterServiceTest {

    @InjectMocks
    private NewsletterService newsletterService;

    @Mock
    private UserNewsletterRepository userNewsletterRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private DomainRepository domainRepository;
    @Mock
    private NewsletterSynchronizer newsletterSynchronizer;
    @Mock
    private UrlRedirectResolver urlRedirectResolver;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    @Mock
    private TransactionTemplate transactionTemplate;

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
        ReflectionTestUtils.setField(userNewsletter, "isRead", true);

        given(userNewsletterRepository.findByIdAndUser_Id(userNewsletterId, userId))
                .willReturn(Optional.of(userNewsletter));

        // when
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
        ReflectionTestUtils.setField(userNewsletter, "isRead", false);

        given(userNewsletterRepository.findByIdAndUser_Id(userNewsletterId, userId))
                .willReturn(Optional.of(userNewsletter));

        // when
        SimpleViewNewsletterResponse response = newsletterService.simpleViewUserNewsletter(userId, userNewsletterId);

        // then
        assertThat(response.newsletterSimpleSummary()).isEmpty();
        assertThat(response.isRead()).isTrue();
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

        // when
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

        // when
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
        ReflectionTestUtils.setField(userNewsletter, "isRead", true);

        given(userNewsletterRepository.findByIdAndUser_Id(userNewsletterId, userId))
                .willReturn(Optional.of(userNewsletter));

        // when
        newsletterService.updateIsRead(userId, userNewsletterId);

        // then
        assertThat(userNewsletter.isRead()).isTrue();
        verify(userNewsletterRepository).save(userNewsletter);
    }

    @Test
    @DisplayName("뉴스레터 생성 성공: 새로운 URL 요청 시 PENDING 상태로 저장되고 분석 이벤트가 발행된다")
    void generateNewsletter_Success_New() {
        // given
        Long userId = 1L;
        String url = "https://example.com/news";
        String memo = "테스트 메모";

        User user = User.builder().build();
        ReflectionTestUtils.setField(user, "id", userId);

        Domain domain = Domain.builder().name("Example").build();
        Newsletter newsletter = Newsletter.createPending(domain, url);
        ReflectionTestUtils.setField(newsletter, "id", 500L);

        given(urlRedirectResolver.resolveIfShortUrl(any())).willReturn(url);

        given(transactionTemplate.execute(any())).willAnswer(invocation -> {
            // invocation.getArgument(0)을 통해 전달된 TransactionCallback을 꺼냅니다.
            TransactionCallback<?> callback = invocation.getArgument(0);
            // 콜백의 로직을 즉시 실행하여 결과를 반환합니다.
            return callback.doInTransaction(null);
        });

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(newsletterSynchronizer.getOrCreateDomain(any())).willReturn(domain);
        given(newsletterSynchronizer.getOrCreatePendingNewsletter(eq(domain), eq(url))).willReturn(newsletter);
        given(userNewsletterRepository.existsByUserAndNewsletter(user, newsletter)).willReturn(false);

        // when
        GenerateNewsletterResponse response = newsletterService.generateNewsletter(userId, url, memo);

        // then
        assertThat(response.llmStatus()).isEqualTo("PENDING");
        verify(applicationEventPublisher).publishEvent(any(NewsletterProcessRequestedEvent.class));

        // [Insight] argThat을 통해 실제 객체의 내부 값들이 서비스 로직을 통해 어떻게 구성되었는지 정밀 검증합니다.
        verify(userNewsletterRepository).save(argThat(un ->
                un.getUser().equals(user) &&
                        un.getNewsletter().equals(newsletter) &&
                        un.getMemo().equals(memo)
        ));
    }
}