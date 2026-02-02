package com.archiveat.server.domain.explore.service;

import com.archiveat.server.domain.explore.dto.response.ExploreResponse;
import com.archiveat.server.domain.explore.dto.response.TopicNewslettersResponse;
import com.archiveat.server.domain.explore.entity.Category;
import com.archiveat.server.domain.explore.entity.Topic;
import com.archiveat.server.domain.explore.repository.SampleCategoryRepository;
import com.archiveat.server.domain.explore.repository.TopicRepository;
import com.archiveat.server.domain.newsletter.entity.UserNewsletter;
import com.archiveat.server.domain.newsletter.repository.UserNewsletterRepository;
import com.archiveat.server.global.common.constant.LlmStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExploreService {

    private final SampleCategoryRepository categoryRepository;
    private final UserNewsletterRepository userNewsletterRepository;
    private final TopicRepository topicRepository;

    @Transactional(readOnly = true)
    public ExploreResponse getExploreData(Long userId) {
        // 1. 인박스 개수 조회 (is_confirmed = false)
        int inboxCount = userNewsletterRepository.countByUserIdAndIsConfirmedFalse(userId);

        // 2. 유저의 토픽별 뉴스레터 개수 조회 및 Map으로 변환 (Key: topicId, Value: count)
        Map<Long, Long> topicCountMap = userNewsletterRepository.countNewslettersByTopicForUser(userId)
                .stream()
                .collect(Collectors.toMap(
                        obj -> (Long) obj[0],
                        obj -> (Long) obj[1]
                ));

        // 3. 전체 카테고리 및 토픽 구조 조회
        List<Category> allCategories = categoryRepository.findAll();

        // 4. DTO 조립
        List<ExploreResponse.CategoryExploreResponse> categories = allCategories.stream()
                .map(category -> new ExploreResponse.CategoryExploreResponse(
                        category.getId(),
                        category.getName(),
                        category.getTopics().stream()
                                .map(topic -> new ExploreResponse.TopicExploreResponse(
                                        topic.getId(),
                                        topic.getName(),
                                        topicCountMap.getOrDefault(topic.getId(), 0L)
                                ))
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());

        // 5. LLM 상태 결정 기본값 DONE
        LlmStatus currentStatus = LlmStatus.DONE; // todo: 유저별 LLM 상태 로직 추가

        return new ExploreResponse(inboxCount, currentStatus, categories);
    }

    /**
     * 특정 토픽의 뉴스레터 목록을 페이징하여 조회
     */
    @Transactional(readOnly = true)
    public TopicNewslettersResponse getTopicNewsletters(Long userId, Long topicId, Pageable pageable) {
        // 1. 토픽 이름 정보 확인
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new IllegalStateException("Topic not found. id=" + topicId));

        // 2. 해당 토픽의 뉴스레터 슬라이스(Slice) 조회
        Slice<UserNewsletter> newsletterSlice = userNewsletterRepository.findByUserIdAndTopicId(userId, topicId, pageable);

        // 3. 응답 DTO 변환
        List<TopicNewslettersResponse.NewsletterItemResponse> newsletters = newsletterSlice.getContent().stream()
                .map(un -> new TopicNewslettersResponse.NewsletterItemResponse(
                        un.getId(),
                        un.getNewsletter().getTitle(),
                        un.getNewsletter().getThumbnailUrl(),
                        un.isRead(),
                        un.getCreatedAt()
                ))
                .collect(Collectors.toList());

        return new TopicNewslettersResponse(
                topicId,
                topic.getName(),
                newsletterSlice.hasNext(),
                newsletters
        );
    }

}
