package com.archiveat.server.domain.newsletter.service;

import com.archiveat.server.domain.explore.entity.Category;
import com.archiveat.server.domain.explore.entity.Topic;
import com.archiveat.server.domain.explore.repository.TopicNewsletterRepository;
import com.archiveat.server.domain.explore.repository.TopicRepository;
import com.archiveat.server.domain.explore.repository.UserTopicRepository;
import com.archiveat.server.domain.newsletter.dto.response.GenerateNewsletterResponse;
import com.archiveat.server.domain.newsletter.dto.response.PythonSummaryResponse;
import com.archiveat.server.domain.newsletter.dto.response.SimpleViewNewsletterResponse;
import com.archiveat.server.domain.newsletter.dto.response.ViewNewsletterResponse;
import com.archiveat.server.domain.newsletter.entity.Domain;
import com.archiveat.server.domain.newsletter.entity.Newsletter;
import com.archiveat.server.domain.newsletter.entity.UserNewsletter;
import com.archiveat.server.domain.newsletter.event.NewsletterProcessRequestedEvent;
import com.archiveat.server.domain.newsletter.repository.DomainRepository;
import com.archiveat.server.domain.newsletter.repository.NewsletterRepository;
import com.archiveat.server.domain.newsletter.repository.UserNewsletterRepository;
import com.archiveat.server.domain.user.entity.User;
import com.archiveat.server.domain.user.repository.UserRepository;
import com.archiveat.server.global.client.PythonClientService;
import com.archiveat.server.global.common.constant.DepthType;
import com.archiveat.server.global.common.constant.LlmStatus;
import com.archiveat.server.global.common.constant.PerspectiveType;
import com.archiveat.server.global.lock.DistributedLockService;
import com.archiveat.server.global.security.TokenHashUtil;
import com.archiveat.server.global.util.UrlRedirectResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.*;

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
    @Mock
    private UserTopicRepository userTopicRepository;
    @Mock
    private NewsletterRepository newsletterRepository;
    @Mock
    private PythonClientService pythonClientService;
    @Mock
    private DistributedLockService distributedLockService;
    @Mock
    private TokenHashUtil tokenHashUtil;
    @Mock
    private TopicRepository topicRepository;
    @Mock
    private TopicNewsletterRepository topicNewsletterRepository;
    @Mock
    private CacheManager cacheManager;

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

    @Test
    @DisplayName("뉴스레터 생성 성공: 이미 DONE 상태인 뉴스레터 요청 시 이벤트를 발행하지 않고 라벨을 즉시 계산한다")
    void generateNewsletter_Success_Existing_Done() {
        // given
        Long userId = 1L;
        String url = "https://already-analyzed.com";

        User user = User.builder().build();
        ReflectionTestUtils.setField(user, "id", userId);

        Newsletter existingNewsletter = Newsletter.builder()
                .contentUrl(url)
                .llmStatus(LlmStatus.DONE)
                .build();

        ReflectionTestUtils.setField(existingNewsletter, "id", 999L);
        ReflectionTestUtils.setField(existingNewsletter, "category", "기술");
        ReflectionTestUtils.setField(existingNewsletter, "consumptionTimeMin", 15);

        given(urlRedirectResolver.resolveIfShortUrl(any())).willReturn(url);
        given(transactionTemplate.execute(any())).willAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(newsletterSynchronizer.getOrCreateDomain(any())).willReturn(Domain.builder().name("Test").build());
        given(newsletterSynchronizer.getOrCreatePendingNewsletter(any(), eq(url))).willReturn(existingNewsletter);

        // 유저 관심사 카테고리에 '기술'이 포함되어 있다고 가정 (-> NOW 라벨)
        given(userTopicRepository.findCategoryNamesByUserIdAndPerspectiveType(userId, PerspectiveType.NOW))
                .willReturn(List.of("기술"));

        // when
        GenerateNewsletterResponse response = newsletterService.generateNewsletter(userId, url, "메모");

        // then
        assertThat(response.llmStatus()).isEqualTo("DONE");

        // [핵심 검증 1] 이미 DONE 상태이므로 분석 이벤트가 발행되지 않아야 함
        verify(applicationEventPublisher, never()).publishEvent(any(NewsletterProcessRequestedEvent.class));

        // [핵심 검증 2] 라벨이 즉시 계산되어 저장되었는지 확인 (15분 -> DEEP, 관심사 일치 -> NOW)
        verify(userNewsletterRepository).save(argThat(un ->
                un.getDepthType() == DepthType.DEEP &&
                        un.getPerspectiveType() == PerspectiveType.NOW
        ));
    }

    @Test
    @DisplayName("비동기 분석 성공: 유튜브 뉴스레터 분석 요청 시 RUNNING을 거쳐 DONE 상태로 업데이트된다")
    void processNewsletterAsync_Success_YouTube() throws Exception {
        // given
        Long newsletterId = 500L;
        String contentUrl = "https://www.youtube.com/watch?v=test";
        String lockKey = "newsletter:lock:hashed_url";

        Newsletter newsletter = Newsletter.builder()
                .contentUrl(contentUrl)
                .llmStatus(LlmStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(newsletter, "id", newsletterId);

        given(tokenHashUtil.sha256Hex(contentUrl)).willReturn("hashed_url");
        given(distributedLockService.tryLock(lockKey, 1)).willReturn(true);
        given(newsletterRepository.findById(newsletterId)).willReturn(Optional.of(newsletter));

        PythonSummaryResponse mockPythonResponse = createMockYouTubeResponse();
        given(pythonClientService.requestYouTubeSummary(contentUrl))
                .willReturn(CompletableFuture.completedFuture(mockPythonResponse));

        // [Insight] executeWithoutResult는 Consumer<TransactionStatus>를 인자로 사용합니다.
        willAnswer(invocation -> {
            Consumer<TransactionStatus> callback = invocation.getArgument(0);
            callback.accept(null); // status에 null을 전달하여 콜백 내부 로직을 실행시킵니다.
            return null;
        }).given(transactionTemplate).executeWithoutResult(any());

        // [Insight] NPE 방지를 위해 CacheManager가 특정 캐시를 반환하도록 설정 (선택사항이나 권장)
        given(cacheManager.getCache("newsletter")).willReturn(null);

        Topic mockTopic = mock(Topic.class);
        Category mockCategory = mock(Category.class);

        given(mockTopic.getName()).willReturn("AI");
        given(mockTopic.getCategory()).willReturn(mockCategory);
        given(mockCategory.getName()).willReturn("기술");

        given(topicRepository.findByNameAndCategory_Name(anyString(), anyString()))
                .willReturn(Optional.of(mockTopic));

        // when
        newsletterService.processNewsletterAsync(newsletterId, contentUrl);

        // then
        assertThat(newsletter.getLlmStatus()).isEqualTo(LlmStatus.DONE);
        assertThat(newsletter.getTitle()).isEqualTo("Mock YouTube Title");
        verify(distributedLockService).unlock(lockKey);
        verify(userNewsletterRepository).findAllByNewsletter_Id(newsletterId);
    }

    // Helper: Python 서버의 가짜 응답 생성
    private PythonSummaryResponse createMockYouTubeResponse() {
        // 1. Analysis 객체 생성 및 필드 주입
        PythonSummaryResponse.Analysis analysis = new PythonSummaryResponse.Analysis();
        ReflectionTestUtils.setField(analysis, "categoryName", "기술");
        ReflectionTestUtils.setField(analysis, "topicName", "AI");
        ReflectionTestUtils.setField(analysis, "smallCardSummary", "요약본");
        ReflectionTestUtils.setField(analysis, "mediumCardSummary", "상세 요약본");
        ReflectionTestUtils.setField(analysis, "newsletterSummary", List.of());

        // 2. VideoInfo 객체 생성 및 필드 주입
        PythonSummaryResponse.VideoInfo videoInfo = new PythonSummaryResponse.VideoInfo();
        ReflectionTestUtils.setField(videoInfo, "title", "Mock YouTube Title");
        ReflectionTestUtils.setField(videoInfo, "thumbnailUrl", "http://thumb.url");
        // [Insight] DTO 필드가 Integer이므로 600.0(Double)이 아닌 600(Integer)을 넣어야 합니다.
        ReflectionTestUtils.setField(videoInfo, "duration", 600);

        // 3. 최종 Response 객체 생성 및 주입
        PythonSummaryResponse response = new PythonSummaryResponse();
        ReflectionTestUtils.setField(response, "analysis", analysis);
        ReflectionTestUtils.setField(response, "videoInfo", videoInfo);

        return response;
    }

    @Test
    @DisplayName("비동기 분석 실패: AI 서버 에러 발생 시 관련 데이터가 삭제되고 락이 해제된다")
    void processNewsletterAsync_Failure_Cleanup() throws Exception {
        // given
        Long newsletterId = 500L;
        String contentUrl = "https://www.youtube.com/watch?v=fail";
        String lockKey = "newsletter:lock:hashed_url";

        Newsletter newsletter = Newsletter.builder()
                .contentUrl(contentUrl)
                .llmStatus(LlmStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(newsletter, "id", newsletterId);

        given(tokenHashUtil.sha256Hex(contentUrl)).willReturn("hashed_url");
        given(distributedLockService.tryLock(lockKey, 1)).willReturn(true);
        given(newsletterRepository.findById(newsletterId)).willReturn(Optional.of(newsletter));

        // [Insight] 외부 서버 에러를 시뮬레이션하기 위해 실패하는 CompletableFuture를 설정합니다.
        CompletableFuture<PythonSummaryResponse> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("AI Server Error"));
        given(pythonClientService.requestYouTubeSummary(anyString()))
                .willReturn(future);

        // cleanup(markNewsletterFailed) 로직에 필요한 데이터 모킹
        given(userNewsletterRepository.findAllByNewsletter_Id(newsletterId)).willReturn(List.of());
        given(topicNewsletterRepository.findAllByNewsletterId(newsletterId)).willReturn(List.of());

        given(cacheManager.getCache("newsletter")).willReturn(null);

        // when & then
        // 서비스 내부에서 예외를 다시 던지므로 이를 확인합니다.
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        newsletterService.processNewsletterAsync(newsletterId, contentUrl))
                .isInstanceOf(RuntimeException.class);

        // [핵심 검증 1] 실패 시 markNewsletterFailed가 호출되어 뉴스레터가 삭제되었는지 확인
        verify(newsletterRepository).deleteById(newsletterId);

        // [핵심 검증 2] 예외가 발생하더라도 분산 락은 반드시 해제되어야 합니다.
        verify(distributedLockService).unlock(lockKey);

        // [핵심 검증 3] 실패 시 캐시 무효화가 호출되는지 확인
        verify(cacheManager, atLeastOnce()).getCache("newsletter");
    }
}