package com.archiveat.server.domain.user.service;

import com.archiveat.server.domain.explore.entity.Category;
import com.archiveat.server.domain.explore.entity.Topic;
import com.archiveat.server.domain.explore.entity.UserTopic;
import com.archiveat.server.domain.explore.repository.CategoryRepository;
import com.archiveat.server.domain.explore.repository.TopicRepository;
import com.archiveat.server.domain.explore.repository.UserTopicRepository;
import com.archiveat.server.domain.user.dto.request.OnboardingInfoRequest;
import com.archiveat.server.domain.user.dto.response.OnboardingMetadataResponse;
import com.archiveat.server.domain.user.entity.User;
import com.archiveat.server.domain.user.repository.UserRepository;
import com.archiveat.server.global.common.constant.DepthType;
import com.archiveat.server.global.common.constant.EmploymentType;
import com.archiveat.server.global.common.constant.PerspectiveType;
import com.archiveat.server.global.common.response.ErrorCode;
import com.archiveat.server.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OnboardingServiceTest {

    @InjectMocks
    private OnboardingService onboardingService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserTopicRepository userTopicRepository;

    @Mock
    private TopicRepository topicRepository;

    @Test
    @DisplayName("닉네임 수정 성공")
    void editNickname_Success() {
        // given
        Long userId = 1L;
        User user = new User("test@e.com", "pw", "oldNickname");
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        onboardingService.editNickname(userId, "newNickname");

        // then
        assertThat(user.getNickname()).isEqualTo("newNickname");
    }

    @Test
    @DisplayName("온보딩 메타데이터 조회 성공: 카테고리와 토픽 매핑 검증")
    void getOnboardingMetadata_Success() {
        // given
        Topic topic = Topic.builder().id(10L).name("AI").build();
        Category category = Category.builder().id(1L).name("IT").topics(List.of(topic)).build();
        given(categoryRepository.findAll()).willReturn(List.of(category));

        // when
        OnboardingMetadataResponse response = onboardingService.getOnboardingMetadata();

        // then
        assertThat(response.categories()).hasSize(1);
        assertThat(response.categories().getFirst().name()).isEqualTo("IT");
        assertThat(response.categories().getFirst().topics().getFirst().name()).isEqualTo("AI");
    }

    @Nested
    @DisplayName("온보딩 정보 제출 테스트")
    class SubmitOnboardingInfo {

        @Test
        @DisplayName("성공: 선택한 토픽은 NOW, 미선택 토픽은 FUTURE로 저장된다")
        void submitOnboardingInfo_Success() {
            // given
            Long userId = 1L;
            User user = new User("t@e.com", "p", "nick");

            // 시스템 전체 토픽 2개 준비
            Topic selectedTopic = Topic.builder().id(10L).name("AI").build();
            Topic unselectedTopic = Topic.builder().id(11L).name("Economy").build();

            OnboardingInfoRequest request = new OnboardingInfoRequest(
                    EmploymentType.STUDENT,
                    new OnboardingInfoRequest.AvailabilityRequest(DepthType.LIGHT, DepthType.DEEP, DepthType.LIGHT, DepthType.DEEP),
                    List.of(new OnboardingInfoRequest.CategoryInterestRequest(1L, List.of(10L))) // 10번(AI)만 선택
            );

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(topicRepository.findAll()).willReturn(List.of(selectedTopic, unselectedTopic));

            // when
            onboardingService.submitOnboardingInfo(userId, request);

            // then
            // 1. 기존 관심사 삭제 확인
            verify(userTopicRepository).deleteAllByUserId(userId);

            // 2. 유저 정보 업데이트 확인 (Reflection으로 필드 값 검증 가능)
            assertThat(user.getEmploymentType()).isEqualTo(EmploymentType.STUDENT);

            // 3. 전체 토픽 저장 로직 검증 (ArgumentCaptor 활용)
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<UserTopic>> captor = ArgumentCaptor.forClass(List.class);
            verify(userTopicRepository).saveAll(captor.capture());

            List<UserTopic> savedUserTopics = captor.getValue();
            assertThat(savedUserTopics).hasSize(2);

            // 10번 토픽은 NOW여야 함
            UserTopic ut1 = savedUserTopics.stream().filter(ut -> ut.getTopic().getId().equals(10L)).findFirst().get();
            assertThat(ut1.getPerspectiveType()).isEqualTo(PerspectiveType.NOW);

            // 11번 토픽은 FUTURE여야 함
            UserTopic ut2 = savedUserTopics.stream().filter(ut -> ut.getTopic().getId().equals(11L)).findFirst().get();
            assertThat(ut2.getPerspectiveType()).isEqualTo(PerspectiveType.FUTURE);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 유저 ID인 경우 USER_NOT_FOUND 예외 발생")
        void submitOnboardingInfo_Fail_UserNotFound() {
            // given
            given(userRepository.findById(anyLong())).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> onboardingService.submitOnboardingInfo(1L, null))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.USER_NOT_FOUND.getMessage());
        }
    }
}