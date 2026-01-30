package com.archiveat.server.domain.user.service;

import com.archiveat.server.domain.explore.entity.Category;
import com.archiveat.server.domain.explore.entity.Topic;
import com.archiveat.server.domain.explore.entity.UserTopic;
import com.archiveat.server.domain.explore.repository.SampleCategoryRepository;
import com.archiveat.server.domain.explore.repository.TopicRepository;
import com.archiveat.server.domain.explore.repository.UserTopicRepository;
import com.archiveat.server.domain.user.dto.request.OnboardingInfoRequest;
import com.archiveat.server.domain.user.dto.response.NicknameResponse;
import com.archiveat.server.domain.user.dto.response.OnboardingMetadataResponse;
import com.archiveat.server.domain.user.entity.User;
import com.archiveat.server.domain.user.repository.UserRepository;
import com.archiveat.server.global.common.constant.AvailabilityType;
import com.archiveat.server.global.common.constant.EmploymentType;
import com.archiveat.server.global.common.constant.PerspectiveType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class OnboardingService {
    private final UserRepository userRepository;
    private final SampleCategoryRepository categoryRepository;
    private final UserTopicRepository userTopicRepository; // 추가
    private final TopicRepository topicRepository;

    // 닉네임 수정
    @Transactional
    public void editNickname(Long userId, String newNickname) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found. id=" + userId));

        user.updateNickname(newNickname);
    }

    // 닉네임 조회
    @Transactional(readOnly = true)
    public NicknameResponse getNickname(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found. id=" + userId));

        return new NicknameResponse(user.getNickname());
    }

    // 온보딩 메타데이터 조회
    @Transactional(readOnly = true)
    public OnboardingMetadataResponse getOnboardingMetadata() {
        // 1. 직업 군 및 시간대 옵션을 Enum에서 추출하여 String 리스트로 변환
        List<String> employmentTypes = Arrays.stream(EmploymentType.values())
                .map(Enum::name)
                .collect(Collectors.toList());

        List<String> availabilityOptions = Arrays.stream(AvailabilityType.values())
                .map(Enum::name)
                .collect(Collectors.toList());

        // 2. DB에서 모든 카테고리와 연관된 토픽 정보를 조회
        List<Category> categories = categoryRepository.findAll();

        // 3. Entity 리스트를 Response DTO 구조로 매핑
        List<OnboardingMetadataResponse.CategoryMetadataResponse> categoryMetadata = categories.stream()
                .map(category -> new OnboardingMetadataResponse.CategoryMetadataResponse(
                        category.getId(),
                        category.getName(),
                        category.getTopics().stream() // Category-Topic은 OneToMany 관계라고 가정합니다.
                                .map(topic -> new OnboardingMetadataResponse.TopicMetadataResponse(
                                        topic.getId(),
                                        topic.getName()
                                ))
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());

        return new OnboardingMetadataResponse(
                employmentTypes,
                availabilityOptions,
                categoryMetadata
        );
    }

    @Transactional
    public void submitOnboardingInfo(Long userId, OnboardingInfoRequest request) {
        // 1. 유저 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found. id=" + userId));

        // 2. 유저 기본 정보(직업군, 시간대 선호도) 업데이트
        user.updateOnboardingInfo(request.employmentType(), request.availability());

        // 3. 기존 관심사(UserTopic) 삭제 (Delete All)
        // 새로운 정책에 따라 전체 지도를 다시 그려야 하므로 기존 데이터를 초기화합니다.
        userTopicRepository.deleteAllByUserId(userId);

        // 4. 시스템 내 모든 토픽 조회
        // 모든 유저에게 전체 토픽에 대한 NOW/FUTURE 상태를 부여하기 위해 기준 데이터를 가져옵니다.
        List<Topic> allTopics = topicRepository.findAll();

        // 5. 요청된(선택된) 토픽 ID들을 Set으로 변환
        // 조회 성능을 높이기 위해 List를 Set으로 변환하여 포함 여부(contains) 확인을 최적화합니다.
        Set<Long> selectedTopicIds = request.interests().stream()
                .flatMap(interest -> interest.topicIds().stream())
                .collect(Collectors.toSet());

        // 6. 모든 토픽을 유저 관심사(UserTopic)로 매핑
        List<UserTopic> userTopics = allTopics.stream()
                .map(topic -> {
                    // 선택된 ID 셋에 포함되어 있으면 NOW, 없으면 FUTURE로 상태를 결정합니다.
                    PerspectiveType type = selectedTopicIds.contains(topic.getId())
                            ? PerspectiveType.NOW
                            : PerspectiveType.FUTURE;

                    return new UserTopic(user, topic, type);
                })
                .collect(Collectors.toList());

        // 7. 일괄 저장 (Insert All)
        userTopicRepository.saveAll(userTopics);
    }

}
