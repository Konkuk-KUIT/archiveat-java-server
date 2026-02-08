package com.archiveat.server.domain.explore.service;

import com.archiveat.server.domain.explore.dto.response.ExploreResponse;
import com.archiveat.server.domain.explore.dto.response.InboxResponse;
import com.archiveat.server.domain.explore.dto.response.TopicNewslettersResponse;
import com.archiveat.server.domain.explore.entity.Topic;
import com.archiveat.server.domain.explore.repository.CategoryRepository;
import com.archiveat.server.domain.explore.repository.TopicRepository;
import com.archiveat.server.domain.newsletter.entity.Newsletter;
import com.archiveat.server.domain.newsletter.entity.UserNewsletter;
import com.archiveat.server.domain.newsletter.repository.UserNewsletterRepository;
import com.archiveat.server.global.common.constant.LlmStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
        Object[] topicCount1 = {101L, 5L};
        List<Object[]> mockTopicCounts = Collections.singletonList(topicCount1);

        when(userNewsletterRepository.countByUserIdAndIsConfirmedFalse(userId)).thenReturn(3);
        when(userNewsletterRepository.countNewslettersByTopicForUser(userId)).thenReturn(mockTopicCounts);
        when(categoryRepository.findAll()).thenReturn(List.of());

        // when
        ExploreResponse response = exploreService.getExploreData(userId);

        // then
        assertThat(response.inboxCount()).isEqualTo(3);
        assertThat(response.categories()).isEmpty();
    }

    @Test
    @DisplayName("토픽별 뉴스레터 목록 페이징 조회 성공")
    void getTopicNewsletters_Success() {
        // given
        Long userId = 1L;
        Long topicId = 10L;
        PageRequest pageable = PageRequest.of(0, 10);

        // [Insight] 엔티티 클래스에 @Builder가 없어 mock 객체를 활용합니다.
        Topic mockTopic = mock(Topic.class);
        // [Reason] 서비스 로직은 파라미터로 넘어온 topicId를 직접 사용하므로 getId() stubbing은 삭제합니다.
        when(mockTopic.getName()).thenReturn("개발");

        Newsletter mockNewsletter = mock(Newsletter.class);
        when(mockNewsletter.getTitle()).thenReturn("테크 소식");

        UserNewsletter mockUn = mock(UserNewsletter.class);
        when(mockUn.getId()).thenReturn(100L);
        when(mockUn.getNewsletter()).thenReturn(mockNewsletter);
        when(mockUn.getCreatedAt()).thenReturn(LocalDateTime.now());

        Slice<UserNewsletter> mockSlice = new SliceImpl<>(List.of(mockUn), pageable, false);

        when(topicRepository.findById(topicId)).thenReturn(Optional.of(mockTopic));
        when(userNewsletterRepository.findByUserIdAndTopicId(userId, topicId, pageable)).thenReturn(mockSlice);

        // when
        TopicNewslettersResponse response = exploreService.getTopicNewsletters(userId, topicId, pageable);

        // then
        assertThat(response.topicName()).isEqualTo("개발");
        assertThat(response.newsletters()).hasSize(1);
        // [Insight] DTO에 전달된 topicId가 우리가 기대한 값과 일치하는지도 검증하면 더 좋습니다.
        assertThat(response.topicId()).isEqualTo(topicId);
    }

    @Test
    @DisplayName("인박스 조회 시 날짜별 그룹화 검증")
    void getInbox_Grouping_Success() {
        // given
        Long userId = 1L;
        LocalDateTime day1 = LocalDateTime.of(2024, 1, 20, 10, 0);
        LocalDateTime day2 = LocalDateTime.of(2024, 1, 21, 10, 0);

        // [Insight] Service의 getInbox 로직은 엔티티의 연관 관계(Newsletter, Domain)를 깊게 참조합니다.
        // 각 엔티티를 Mock으로 만들고 상위 Mock이 하위 Mock을 반환하도록 연결해줍니다.
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
        assertThat(response.inbox().get(0).date()).isEqualTo("2024-01-21"); // 최신순 정렬 확인
    }
}