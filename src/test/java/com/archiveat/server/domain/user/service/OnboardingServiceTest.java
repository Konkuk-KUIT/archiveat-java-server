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
import org.springframework.test.util.ReflectionTestUtils;

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
        Topic topic = Topic.builder().name("AI").build();
        ReflectionTestUtils.setField(topic, "id", 10L);

        Category category = Category.builder().name("IT").topics(List.of(topic)).build();
        ReflectionTestUtils.setField(category, "id", 1L);

        given(categoryRepository.findAll()).willReturn(List.of(category));

        // when
        OnboardingMetadataResponse response = onboardingService.getOnboardingMetadata();

        // then
        assertThat(response.categories()).hasSize(1);
        assertThat(response.categories().get(0).name()).isEqualTo("IT");
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
            Topic selectedTopic = Topic.builder().name("AI").build();
            ReflectionTestUtils.setField(selectedTopic, "id", 10L);

            Topic unselectedTopic = Topic.builder().name("Economy").build();
            ReflectionTestUtils.setField(unselectedTopic, "id", 11L);

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
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<UserTopic>> captor = ArgumentCaptor.forClass(List.class);
            verify(userTopicRepository).saveAll(captor.capture());
            List<UserTopic> savedUserTopics = captor.getValue();

            assertThat(savedUserTopics)
                    .filteredOn(ut -> ut.getTopic().getId().equals(10L))
                    .singleElement()
                    .extracting(UserTopic::getPerspectiveType)
                    .isEqualTo(PerspectiveType.NOW);

            assertThat(savedUserTopics)
                    .filteredOn(ut -> ut.getTopic().getId().equals(11L))
                    .singleElement()
                    .extracting(UserTopic::getPerspectiveType)
                    .isEqualTo(PerspectiveType.FUTURE);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 유저 ID인 경우 USER_NOT_FOUND 예외 발생")
        void submitOnboardingInfo_Fail_UserNotFound() {
            OnboardingInfoRequest dummyRequest = new OnboardingInfoRequest(EmploymentType.STUDENT, null, List.of());
            given(userRepository.findById(anyLong())).willReturn(Optional.empty());

            assertThatThrownBy(() -> onboardingService.submitOnboardingInfo(1L, dummyRequest))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.USER_NOT_FOUND.getMessage());
        }
    }
}