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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.anyLong;
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

    @Test
    @DisplayName("탐색 탭 메인 데이터 조회 테스트")
    void getExploreData_Success() {
        // given
        Long userId = 1L;

        // Stubbing: Category 기술
        Category techCategory = mock(Category.class);
        when(techCategory.getId()).thenReturn(1L);
        when(techCategory.getName()).thenReturn("기술");

        // Stubbing: Topic AI
        Topic aiTopic = mock(Topic.class);
        when(aiTopic.getId()).thenReturn(101L);
        when(aiTopic.getName()).thenReturn("AI");

        // Stubbing: Topic 개발
        Topic devTopic = mock(Topic.class);
        when(devTopic.getId()).thenReturn(102L);
        when(devTopic.getName()).thenReturn("개발");

        // Stubbing: 관계 설정
        when(techCategory.getTopics()).thenReturn(List.of(aiTopic, devTopic));
        when(categoryRepository.findAll()).thenReturn(List.of(techCategory));

        // 토픽별 뉴스레터 개수 데이터 설정
        List<Object[]> mockTopicCounts = new ArrayList<>();
        mockTopicCounts.add(new Object[]{101L, 5L}); // AI 토픽(101)에 5개

        when(userNewsletterRepository.countByUserIdAndIsConfirmedFalse(userId)).thenReturn(3);
        when(userNewsletterRepository.countNewslettersByTopicForUser(userId)).thenReturn(mockTopicCounts);

        // when
        ExploreResponse response = exploreService.getExploreData(userId);

        // then
        assertSoftly(softly -> {
            softly.assertThat(response.inboxCount()).as("인박스 미확인 개수").isEqualTo(3);
            softly.assertThat(response.llmStatus()).isEqualTo(LlmStatus.DONE); // TODO: Service쪽에 현재 DONE으로만 반환하도록 하드코딩 되어있음

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
    @DisplayName("토픽별 뉴스레터 목록 페이징 조회 성공")
    void getTopicNewsletters_Success() {
        // given
        Long userId = 1L;
        Long topicId = 10L;
        PageRequest pageable = PageRequest.of(0, 10);

        Topic mockTopic = mock(Topic.class);
        when(mockTopic.getName()).thenReturn("개발");

        Newsletter mockN = mock(Newsletter.class);
        when(mockN.getTitle()).thenReturn("테크 소식");

        UserNewsletter mockUn = mock(UserNewsletter.class);
        when(mockUn.getId()).thenReturn(100L);
        when(mockUn.getNewsletter()).thenReturn(mockN);
        when(mockUn.getCreatedAt()).thenReturn(LocalDateTime.now());

        // 다음 페이지가 있는 상황(true)을 시뮬레이션하여 FE가 '더보기' 버튼을 띄울 수 있는지 검증합니다.
        Slice<UserNewsletter> mockSlice = new SliceImpl<>(List.of(mockUn), pageable, true);

        when(topicRepository.findById(topicId)).thenReturn(Optional.of(mockTopic));
        when(userNewsletterRepository.findByUserIdAndTopicId(userId, topicId, pageable)).thenReturn(mockSlice);

        // when
        TopicNewslettersResponse response = exploreService.getTopicNewsletters(userId, topicId, pageable);

        // then
        assertSoftly(softly -> {
            softly.assertThat(response.topicName()).isEqualTo("개발");
            softly.assertThat(response.newsletters()).hasSize(1);
            softly.assertThat(response.hasNext()).isTrue();
        });
    }

    @Test
    @DisplayName("인박스 조회 시 날짜별 그룹화 검증")
    void getInbox_Grouping_Success() {
        // given
        Long userId = 1L;
        LocalDateTime day1 = LocalDateTime.of(2024, 1, 20, 10, 0);
        LocalDateTime day2 = LocalDateTime.of(2024, 1, 21, 10, 0);

        Newsletter mockN1 = mock(Newsletter.class);
        when(mockN1.getTitle()).thenReturn("N1");
        when(mockN1.getLlmStatus()).thenReturn(LlmStatus.DONE);

        UserNewsletter mockUn1 = mock(UserNewsletter.class);
        when(mockUn1.getId()).thenReturn(1L);
        when(mockUn1.getNewsletter()).thenReturn(mockN1);
        when(mockUn1.getCreatedAt()).thenReturn(day1);

        Newsletter mockN2 = mock(Newsletter.class);
        when(mockN2.getTitle()).thenReturn("N2");
        when(mockN2.getLlmStatus()).thenReturn(LlmStatus.DONE);

        UserNewsletter mockUn2 = mock(UserNewsletter.class);
        when(mockUn2.getId()).thenReturn(2L);
        when(mockUn2.getNewsletter()).thenReturn(mockN2);
        when(mockUn2.getCreatedAt()).thenReturn(day2);

        when(userNewsletterRepository.findAllInboxByUserId(userId)).thenReturn(List.of(mockUn2, mockUn1));
        when(categoryRepository.findAll()).thenReturn(List.of());
        when(topicRepository.findAll()).thenReturn(List.of());

        // when
        InboxResponse response = exploreService.getInbox(userId);

        // then
        // [Reason] InboxResponse는 'inbox'라는 필드명으로 그룹화된 데이터를 관리합니다.
        assertThat(response.inbox()).hasSize(2);
        assertThat(response.inbox().getFirst().date()).isEqualTo("2024-01-21"); // 최신순 정렬 확인
    }

    @Test
    @DisplayName("인박스 조회 시 LLM 상태에 따른 카테고리/토픽 노출 제어 검증")
    void getInbox_StatusHandling_Success() {
        // given
        Long userId = 1L;
        LocalDateTime now = LocalDateTime.now();

        // 1. 분석 완료된 뉴스레터
        Newsletter doneN = mock(Newsletter.class);
        when(doneN.getLlmStatus()).thenReturn(LlmStatus.DONE);
        when(doneN.getCategory()).thenReturn("IT");
        when(doneN.getTopic()).thenReturn("AI");
        UserNewsletter unDone = mock(UserNewsletter.class);
        when(unDone.getNewsletter()).thenReturn(doneN);
        when(unDone.getCreatedAt()).thenReturn(now);

        // 2. 분석 중인 뉴스레터
        Newsletter runningN = mock(Newsletter.class);
        when(runningN.getLlmStatus()).thenReturn(LlmStatus.RUNNING);
        UserNewsletter unRunning = mock(UserNewsletter.class);
        when(unRunning.getNewsletter()).thenReturn(runningN);
        when(unRunning.getCreatedAt()).thenReturn(now);

        when(userNewsletterRepository.findAllInboxByUserId(userId)).thenReturn(List.of(unDone, unRunning));

        // 캐싱 데이터 설정
        Category cat = mock(Category.class);
        when(cat.getName()).thenReturn("IT");
        when(cat.getId()).thenReturn(1L);
        when(categoryRepository.findAll()).thenReturn(List.of(cat));

        Topic top = mock(Topic.class);
        when(top.getName()).thenReturn("AI");
        when(top.getId()).thenReturn(10L);
        when(topicRepository.findAll()).thenReturn(List.of(top));

        // when
        InboxResponse response = exploreService.getInbox(userId);

        // then
        assertSoftly(softly -> {
            List<InboxResponse.InboxItemDto> items = response.inbox().getFirst().items();

            // 분석 완료된 경우: ID와 이름이 존재해야 함
            InboxResponse.InboxItemDto doneItem = items.get(0);
            softly.assertThat(doneItem.category().id()).isEqualTo(1L);

            // 분석 중인 경우: ID와 이름이 null이어야 함
            InboxResponse.InboxItemDto runningItem = items.get(1);
            softly.assertThat(runningItem.category().id()).isNull();
            softly.assertThat(runningItem.topic().id()).isNull();
        });
    }

    @Test
    @DisplayName("인박스 분류 수정 성공: 카테고리/토픽 변경 및 원본 동기화 확인")
    void updateInboxClassification_Success() {
        // given
        Long userId = 1L;
        ClassificationRequest request = new ClassificationRequest(2L, 20L, "메모 수정");

        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(userId);

        Newsletter mockN = mock(Newsletter.class);
        UserNewsletter mockUn = mock(UserNewsletter.class);
        when(mockUn.getUser()).thenReturn(mockUser);
        when(mockUn.getNewsletter()).thenReturn(mockN);
        when(mockUn.getConfirmedAt()).thenReturn(LocalDateTime.now());
        when(mockUn.getModifiedAt()).thenReturn(LocalDateTime.now());

        Category newCat = mock(Category.class);
        when(newCat.getId()).thenReturn(2L);
        when(newCat.getName()).thenReturn("경제");

        Topic newTop = mock(Topic.class);
        when(newTop.getId()).thenReturn(20L);
        when(newTop.getName()).thenReturn("주식");
        when(newTop.getCategory()).thenReturn(newCat);

        when(userNewsletterRepository.findById(anyLong())).thenReturn(Optional.of(mockUn));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(newCat));
        when(topicRepository.findById(20L)).thenReturn(Optional.of(newTop));

        // when
        ClassificationResponse response = exploreService.updateInboxClassification(userId, 100L, request);

        // then
        // [Reason] verify를 사용해 엔티티 내부의 수정 메서드들이 실제로 호출되었는지 검증합니다.
        verify(mockUn).updateClassification("메모 수정");
        verify(mockN).updateCategoryAndTopic("경제", "주식");
        assertThat(response.category().name()).isEqualTo("경제");
    }

    @Test
    @DisplayName("인박스 일괄 확인 처리 호출 검증")
    void confirmAllInbox_Success() {
        // given
        Long userId = 1L;

        // when
        exploreService.confirmAllInbox(userId);

        // then
        verify(userNewsletterRepository)
                .bulkConfirmByUserId(eq(userId), any(LocalDateTime.class), eq(LlmStatus.DONE));
    }
}