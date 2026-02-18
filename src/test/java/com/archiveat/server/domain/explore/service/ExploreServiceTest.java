package com.archiveat.server.domain.explore.service;

import com.archiveat.server.domain.explore.dto.request.ClassificationRequest;
import com.archiveat.server.domain.explore.dto.response.ClassificationResponse;
import com.archiveat.server.domain.explore.dto.response.ExploreResponse;
import com.archiveat.server.domain.explore.dto.response.InboxResponse;
import com.archiveat.server.domain.explore.dto.response.TopicNewslettersResponse;
import com.archiveat.server.domain.explore.entity.Category;
import com.archiveat.server.domain.explore.entity.Topic;
import com.archiveat.server.domain.explore.repository.CategoryRepository;
import com.archiveat.server.domain.explore.repository.TopicRepository;
import com.archiveat.server.domain.newsletter.entity.Newsletter;
import com.archiveat.server.domain.newsletter.entity.UserNewsletter;
import com.archiveat.server.domain.newsletter.repository.UserNewsletterRepository;
import com.archiveat.server.domain.user.entity.User;
import com.archiveat.server.global.common.constant.LlmStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExploreServiceTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private UserNewsletterRepository userNewsletterRepository;
    @Mock
    private TopicRepository topicRepository;

    @InjectMocks
    private ExploreService exploreService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = mock(User.class);
        lenient().when(mockUser.getId()).thenReturn(1L);
    }

    @Test
    @DisplayName("[Explore] 메인 데이터 조회 테스트")
    void getExploreData_Success() {
        // given
        Long userId = 1L;

        Topic aiTopic = Topic.builder()
                .name("AI")
                .build();
        ReflectionTestUtils.setField(aiTopic, "id", 101L);

        Topic devTopic = Topic.builder()
                .name("개발")
                .build();
        ReflectionTestUtils.setField(devTopic, "id", 102L);

        Category techCategory = Category.builder()
                .name("기술")
                .topics(List.of(aiTopic, devTopic)) // 생성자 빌더로 연관관계까지 한 번에 설정
                .build();
        ReflectionTestUtils.setField(techCategory, "id", 1L);

        // Stubbing: 관계 설정
        when(categoryRepository.findAll()).thenReturn(List.of(techCategory));

        // 토픽별 뉴스레터 개수 데이터 설정
        List<Object[]> mockTopicCounts = new ArrayList<>();
        mockTopicCounts.add(new Object[] { 101L, 5L }); // AI 토픽(101)에 5개

        when(userNewsletterRepository.countByUserIdAndIsConfirmedFalse(userId)).thenReturn(3);
        when(userNewsletterRepository.countNewslettersByTopicForUser(userId)).thenReturn(mockTopicCounts);
        when(userNewsletterRepository.existsByUserIdAndNewsletter_LlmStatus(userId, LlmStatus.RUNNING))
                .thenReturn(false);
        when(userNewsletterRepository.existsByUserIdAndNewsletter_LlmStatus(userId, LlmStatus.PENDING))
                .thenReturn(false);

        // when
        ExploreResponse response = exploreService.getExploreData(userId);

        // then
        assertSoftly(softly -> {
            softly.assertThat(response.inboxCount()).as("인박스 미확인 개수").isEqualTo(3);
            softly.assertThat(response.llmStatus()).isEqualTo(LlmStatus.DONE);

            // 카테고리 검증
            softly.assertThat(response.categories()).hasSize(1);
            ExploreResponse.CategoryExploreResponse categoryDto = response.categories().getFirst();
            softly.assertThat(categoryDto.name()).isEqualTo("기술");

            // 토픽 개수 및 매핑 검증
            List<ExploreResponse.TopicExploreResponse> topics = categoryDto.topics();
            softly.assertThat(topics).hasSize(2);

            // 데이터가 존재하는 토픽 (AI)
            ExploreResponse.TopicExploreResponse aiDto = topics.stream()
                    .filter(t -> t.name().equals("AI"))
                    .findFirst().orElse(null);
            softly.assertThat(aiDto).isNotNull();
            softly.assertThat(aiDto.newsletterCount()).isEqualTo(5L);

            // 데이터가 없는 토픽 (개발 - 0개로 반환되어야 함)
            ExploreResponse.TopicExploreResponse devDto = topics.stream()
                    .filter(t -> t.name().equals("개발"))
                    .findFirst().orElse(null);
            softly.assertThat(devDto).isNotNull();
            softly.assertThat(devDto.newsletterCount()).isEqualTo(0L);
        });
    }

    @Test
    @DisplayName("[Explore] 토픽별 뉴스레터 목록 페이징 조회 성공")
    void getTopicNewsletters_Success() {
        // given
        Long userId = 1L;
        Long topicId = 10L;
        PageRequest pageable = PageRequest.of(0, 10);

        // 실제 객체 생성
        Topic topic = Topic.builder().name("개발").build();
        ReflectionTestUtils.setField(topic, "id", topicId);

        Newsletter newsletter = Newsletter.builder()
                .title("테크 소식")
                .contentUrl("http://example.com")
                .build();

        UserNewsletter userNewsletter = UserNewsletter.create(mockUser, newsletter, null, null, null);
        ReflectionTestUtils.setField(userNewsletter, "id", 100L);
        ReflectionTestUtils.setField(userNewsletter, "createdAt", LocalDateTime.now());

        Slice<UserNewsletter> mockSlice = new SliceImpl<>(List.of(userNewsletter), pageable, true);

        when(topicRepository.findById(topicId)).thenReturn(Optional.of(topic));
        when(userNewsletterRepository.findByUserIdAndTopicId(userId, topicId, pageable)).thenReturn(mockSlice);

        // when
        TopicNewslettersResponse response = exploreService.getTopicNewsletters(userId, topicId, pageable);

        // then
        assertSoftly(softly -> {
            softly.assertThat(response.topicName()).isEqualTo("개발");
            softly.assertThat(response.newsletters()).hasSize(1);
            softly.assertThat(response.newsletters().getFirst().title()).isEqualTo("테크 소식");
        });
    }

    @Test
    @DisplayName("[Explore] 인박스 조회 시 날짜별 그룹화 검증")
    void getInbox_Grouping_Success() {
        // given
        Long userId = 1L;
        LocalDateTime day1 = LocalDateTime.of(2024, 1, 20, 10, 0);
        LocalDateTime day2 = LocalDateTime.of(2024, 1, 21, 10, 0);

        // 실제 빌더를 사용하여 Newsletter 생성
        Newsletter n1 = Newsletter.builder()
                .title("N1")
                .contentUrl("http://url1")
                .llmStatus(LlmStatus.DONE)
                .build();
        ReflectionTestUtils.setField(n1, "id", 1L);

        Newsletter n2 = Newsletter.builder()
                .title("N2")
                .contentUrl("http://url2")
                .llmStatus(LlmStatus.DONE)
                .build();
        ReflectionTestUtils.setField(n2, "id", 2L);

        // UserNewsletter 생성 및 Reflection을 통한 필드 주입
        UserNewsletter un1 = UserNewsletter.create(null, n1, null, null, null);
        ReflectionTestUtils.setField(un1, "id", 101L);
        ReflectionTestUtils.setField(un1, "createdAt", day1);

        UserNewsletter un2 = UserNewsletter.create(null, n2, null, null, null);
        ReflectionTestUtils.setField(un2, "id", 102L);
        ReflectionTestUtils.setField(un2, "createdAt", day2);

        // Repository는 실제 객체 리스트를 반환하도록 설정
        when(userNewsletterRepository.findAllInboxByUserId(userId)).thenReturn(List.of(un2, un1));
        when(categoryRepository.findAll()).thenReturn(List.of());
        when(topicRepository.findAll()).thenReturn(List.of());

        // when
        InboxResponse response = exploreService.getInbox(userId);

        // then
        assertThat(response.inbox()).hasSize(2);
        assertThat(response.inbox().getFirst().date()).isEqualTo("2024-01-21"); // 최신순 정렬 확인
    }

    @Test
    @DisplayName("[Explore] 인박스 조회 시 LLM 상태에 따른 카테고리/토픽 노출 제어 검증")
    void getInbox_StatusHandling_Success() {
        // given
        Long userId = 1L;
        LocalDateTime now = LocalDateTime.now();
        Category itCategory = Category.builder().name("IT").build();
        ReflectionTestUtils.setField(itCategory, "id", 1L);
        Topic aiTopic = Topic.builder().name("AI").build();
        ReflectionTestUtils.setField(aiTopic, "id", 10L);

        // 1. 분석 완료된 뉴스레터 (실제 객체)
        Newsletter doneN = Newsletter.builder()
                .contentUrl("url1")
                .llmStatus(LlmStatus.DONE)
                .build();

        UserNewsletter unDone = UserNewsletter.create(null, doneN, itCategory, aiTopic, null);
        ReflectionTestUtils.setField(unDone, "createdAt", now);

        // 2. 분석 중인 뉴스레터 (실제 객체)
        Newsletter runningN = Newsletter.builder()
                .contentUrl("url2")
                .llmStatus(LlmStatus.RUNNING)
                .build();

        UserNewsletter unRunning = UserNewsletter.create(null, runningN, null, null, null);
        ReflectionTestUtils.setField(unRunning, "createdAt", now);

        when(userNewsletterRepository.findAllInboxByUserId(userId)).thenReturn(List.of(unDone, unRunning));
        when(categoryRepository.findAll()).thenReturn(List.of(itCategory));
        when(topicRepository.findAll()).thenReturn(List.of(aiTopic));

        // when
        InboxResponse response = exploreService.getInbox(userId);

        // then
        assertSoftly(softly -> {
            List<InboxResponse.InboxItemDto> items = response.inbox().getFirst().items();
            softly.assertThat(items.get(0).category().id()).isEqualTo(1L); // DONE 상태는 데이터 노출
            softly.assertThat(items.get(1).category().id()).isNull();      // RUNNING 상태는 null 노출
        });
    }

    @Test
    @DisplayName("[Explore] 인박스 분류 수정 성공: 카테고리/토픽 변경 및 확인")
    void updateInboxClassification_Success() {
        // given
        Long userId = 1L;
        ClassificationRequest request = new ClassificationRequest(2L, 20L, "메모 수정");

        User mockUser = mock(User.class); // User는 모킹 유지
        when(mockUser.getId()).thenReturn(userId);

        Newsletter newsletter = Newsletter.builder()
                .title("제목")
                .contentUrl("http://url")
                .llmStatus(LlmStatus.DONE)
                .build();
        ReflectionTestUtils.setField(newsletter, "id", 1L);

        // 실제 객체를 생성하고 행위 검증을 위해 spy로 감싸기
        UserNewsletter userNewsletter = spy(UserNewsletter.create(mockUser, newsletter, null, null, "원본메모"));
        ReflectionTestUtils.setField(userNewsletter, "id", 100L);
        ReflectionTestUtils.setField(userNewsletter, "modifiedAt", LocalDateTime.now());

        // 새로운 카테고리와 토픽 빌더로 생성
        Category newCat = Category.builder().name("경제").build();
        ReflectionTestUtils.setField(newCat, "id", 2L);
        Topic newTop = Topic.builder().name("주식").category(newCat).build();
        ReflectionTestUtils.setField(newTop, "id", 20L);

        when(userNewsletterRepository.findById(100L)).thenReturn(Optional.of(userNewsletter));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(newCat));
        when(topicRepository.findById(20L)).thenReturn(Optional.of(newTop));

        // when
        ClassificationResponse response = exploreService.updateInboxClassification(userId, 100L, request);

        // then
        // 엔티티 내부 메서드가 호출되었는지 실제 동작 확인
        verify(userNewsletter).updateClassification(any(Category.class), any(Topic.class), eq("메모 수정"));
        assertThat(response.category().name()).isEqualTo("경제");
    }

    @Test
    @DisplayName("[Explore] 인박스 일괄 확인 처리 호출 검증")
    void confirmAllInbox_Success() {
        // given
        Long userId = 1L;

        // when
        exploreService.confirmAllInbox(userId);

        // then
        verify(userNewsletterRepository)
                .bulkConfirmByUserId(eq(userId), any(LocalDateTime.class), eq(LlmStatus.DONE));
    }

    @Test
    @DisplayName("getExploreData returns RUNNING status when user has running newsletter")
    void getExploreData_ReturnsRunningStatus_WhenRunningNewsletterExists() {
        // given
        Long userId = 1L;
        when(userNewsletterRepository.countByUserIdAndIsConfirmedFalse(userId)).thenReturn(0);
        when(userNewsletterRepository.countNewslettersByTopicForUser(userId)).thenReturn(List.of());
        when(categoryRepository.findAll()).thenReturn(List.of());
        when(userNewsletterRepository.existsByUserIdAndNewsletter_LlmStatus(userId, LlmStatus.RUNNING))
                .thenReturn(true);

        // when
        ExploreResponse response = exploreService.getExploreData(userId);

        // then
        assertThat(response.llmStatus()).isEqualTo(LlmStatus.RUNNING);
    }

    @Test
    @DisplayName("getExploreData returns DONE status when user has no running or pending newsletter")
    void getExploreData_ReturnsDoneStatus_WhenNoRunningNewsletterExists() {
        // given
        Long userId = 1L;
        when(userNewsletterRepository.countByUserIdAndIsConfirmedFalse(userId)).thenReturn(0);
        when(userNewsletterRepository.countNewslettersByTopicForUser(userId)).thenReturn(List.of());
        when(categoryRepository.findAll()).thenReturn(List.of());
        when(userNewsletterRepository.existsByUserIdAndNewsletter_LlmStatus(userId, LlmStatus.RUNNING))
                .thenReturn(false);
        when(userNewsletterRepository.existsByUserIdAndNewsletter_LlmStatus(userId, LlmStatus.PENDING))
                .thenReturn(false);

        // when
        ExploreResponse response = exploreService.getExploreData(userId);

        // then
        assertThat(response.llmStatus()).isEqualTo(LlmStatus.DONE);
    }

    @Test
    @DisplayName("getExploreData returns RUNNING status when user has pending newsletter")
    void getExploreData_ReturnsRunningStatus_WhenPendingNewsletterExists() {
        // given
        Long userId = 1L;
        when(userNewsletterRepository.countByUserIdAndIsConfirmedFalse(userId)).thenReturn(0);
        when(userNewsletterRepository.countNewslettersByTopicForUser(userId)).thenReturn(List.of());
        when(categoryRepository.findAll()).thenReturn(List.of());
        when(userNewsletterRepository.existsByUserIdAndNewsletter_LlmStatus(userId, LlmStatus.RUNNING))
                .thenReturn(false);
        when(userNewsletterRepository.existsByUserIdAndNewsletter_LlmStatus(userId, LlmStatus.PENDING))
                .thenReturn(true);

        // when
        ExploreResponse response = exploreService.getExploreData(userId);

        // then
        assertThat(response.llmStatus()).isEqualTo(LlmStatus.RUNNING);
    }

    @Test
    @DisplayName("getExploreData handles null topic IDs without NPE")
    void getExploreData_NullTopicInCount_NoException() {
        // given
        Long userId = 1L;

        // topic이 null인 UserNewsletter가 있을 때 쿼리 결과에 null topicId 포함
        List<Object[]> mockTopicCounts = new ArrayList<>();
        mockTopicCounts.add(new Object[] { null, 2L }); // null topic (LLM 미처리)
        mockTopicCounts.add(new Object[] { 101L, 3L }); // 정상 topic

        when(userNewsletterRepository.countByUserIdAndIsConfirmedFalse(userId)).thenReturn(1);
        when(userNewsletterRepository.countNewslettersByTopicForUser(userId)).thenReturn(mockTopicCounts);
        when(categoryRepository.findAll()).thenReturn(List.of());
        when(userNewsletterRepository.existsByUserIdAndNewsletter_LlmStatus(userId, LlmStatus.RUNNING))
                .thenReturn(true);

        // when — NPE 없이 정상 동작해야 함
        ExploreResponse response = exploreService.getExploreData(userId);

        // then
        assertThat(response.llmStatus()).isEqualTo(LlmStatus.RUNNING);
        assertThat(response.inboxCount()).isEqualTo(1);
    }

}