package com.archiveat.server.domain.explore.service;

import com.archiveat.server.domain.explore.dto.request.ClassificationRequest;
import com.archiveat.server.domain.explore.dto.response.*;
import com.archiveat.server.domain.explore.entity.Category;
import com.archiveat.server.domain.explore.entity.Topic;
import com.archiveat.server.domain.explore.repository.CategoryRepository;
import com.archiveat.server.domain.explore.repository.TopicRepository;
import com.archiveat.server.domain.newsletter.entity.Newsletter;
import com.archiveat.server.domain.newsletter.entity.UserNewsletter;
import com.archiveat.server.domain.newsletter.repository.UserNewsletterRepository;
import com.archiveat.server.global.common.constant.LlmStatus;
import com.archiveat.server.global.common.response.ErrorCode;
import com.archiveat.server.global.exception.CustomException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.archiveat.server.global.common.constant.DateTimeConstant.*;

@Service
@RequiredArgsConstructor
public class ExploreService {

    private final CategoryRepository categoryRepository;
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
                        obj -> ((Number) obj[0]).longValue(), // topicId
                        obj -> ((Number) obj[1]).longValue()  // newsletterCount
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

    @Transactional(readOnly = true)
    public InboxResponse getInbox(Long userId) {
        // 1. 인박스 아이템 조회
        List<UserNewsletter> userNewsletters = userNewsletterRepository.findAllInboxByUserId(userId);

        // 2. 캐싱 로직: 카테고리 및 토픽 정보를 미리 조회하여 Map으로 저장
        Map<String, InboxResponse.CategoryDto> categoryMap = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(Category::getName,
                        c -> new InboxResponse.CategoryDto(c.getId(), c.getName()), (existing, replacement) -> existing));

        Map<String, InboxResponse.TopicDto> topicMap = topicRepository.findAll().stream()
                .collect(Collectors.toMap(Topic::getName,
                        t -> new InboxResponse.TopicDto(t.getId(), t.getName()), (existing, replacement) -> existing));

        // 3. 날짜별 그룹화 및 DTO 변환
        Map<String, List<UserNewsletter>> groupedByDate = userNewsletters.stream()
                .collect(Collectors.groupingBy(
                        un -> un.getCreatedAt().toLocalDate().toString(),
                        Collectors.toList()
                ));

        List<InboxResponse.InboxDateGroupDto> inboxGroups = groupedByDate.keySet().stream()
                .sorted((d1, d2) -> d2.compareTo(d1))
                .map(date -> InboxResponse.InboxDateGroupDto.builder()
                        .date(date)
                        .items(groupedByDate.get(date).stream()
                                // 캐싱된 Map을 인자로 넘겨 DB 추가 조회를 막습니다.
                                .map(un -> convertToItemDto(un, categoryMap, topicMap))
                                .collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());

        return InboxResponse.builder().inbox(inboxGroups).build();
    }

    /**
     * UserNewsletter 엔티티를 InboxItemDto(Record)로 변환합니다.
     */
    private InboxResponse.InboxItemDto convertToItemDto(
            UserNewsletter un,
            Map<String, InboxResponse.CategoryDto> categoryMap,
            Map<String, InboxResponse.TopicDto> topicMap
    ) {
        Newsletter n = un.getNewsletter();

        InboxResponse.CategoryDto categoryDto = (n.getLlmStatus() == LlmStatus.DONE)
                ? categoryMap.getOrDefault(n.getCategory(), new InboxResponse.CategoryDto(null, null))
                : new InboxResponse.CategoryDto(null, null);

        InboxResponse.TopicDto topicDto = (n.getLlmStatus() == LlmStatus.DONE)
                ? topicMap.getOrDefault(n.getTopic(), new InboxResponse.TopicDto(null, null))
                : new InboxResponse.TopicDto(null, null);

        return InboxResponse.InboxItemDto.builder()
                .userNewsletterId(un.getId())
                .llmStatus(n.getLlmStatus())
                .contentUrl(n.getContentUrl())
                .title(n.getTitle())
                .domainName(n.getDomain() != null ? n.getDomain().getName() : null)
                .createdAt(un.getCreatedAt().atZone(APP_ZONE).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                .category(categoryDto)
                .topic(topicDto)
                .build();
    }

    @Transactional
    public ClassificationResponse updateInboxClassification(Long userId, Long userNewsletterId, ClassificationRequest request) {
        // 1. 수정하려는 인박스 아이템(UserNewsletter)을 조회합니다.
        UserNewsletter userNewsletter = userNewsletterRepository.findById(userNewsletterId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NEWSLETTER_NOT_FOUND));

        if (!userNewsletter.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.USER_NEWSLETTER_NOT_AUTHORIZED);
        }

        // 2. 요청된 ID를 기반으로 카테고리와 토픽 정보를 DB에서 조회합니다.
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new CustomException(ErrorCode.CATEGORY_NOT_FOUND));

        Topic topic = topicRepository.findById(request.topicId())
                .orElseThrow(() -> new CustomException(ErrorCode.TOPIC_NOT_FOUND));

        // 3. 엔티티 상태를 업데이트합니다. (도메인 메서드 활용)
        userNewsletter.updateClassification(request.memo());

        // 4. 원본 Newsletter의 분류 정보도 사용자가 수정한 값으로 동기화합니다.
        Newsletter newsletter = userNewsletter.getNewsletter();
        newsletter.updateCategoryAndTopic(category.getName(), topic.getName());

        // 5. 응답 DTO 조립
        return ClassificationResponse.builder()
                .userNewsletterId(userNewsletter.getId())
                .newsletterId(newsletter.getId())
                .category(new ClassificationResponse.CategoryDto(category.getId(), category.getName()))
                .topic(new ClassificationResponse.TopicDto(topic.getId(), topic.getName()))
                .memo(userNewsletter.getMemo())
                .classificationConfirmedAt(userNewsletter.getConfirmedAt()
                        .atZone(APP_ZONE)
                        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                .modifiedAt(userNewsletter.getModifiedAt()
                        .atZone(APP_ZONE)
                        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                .build();
    }

    @Transactional(readOnly = true)
    public InboxEditResponse getInboxEditData(Long userId, Long userNewsletterId) {
        // 1. 인박스 아이템 조회 및 소유권 검증
        UserNewsletter userNewsletter = userNewsletterRepository.findById(userNewsletterId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NEWSLETTER_NOT_FOUND));

        if (!userNewsletter.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.USER_NEWSLETTER_NOT_AUTHORIZED);
        }

        Newsletter newsletter = userNewsletter.getNewsletter();

        // 2. 현재 설정된 카테고리/토픽의 ID 조회 (String -> Long)
        Long currentCategoryId = categoryRepository.findByName(newsletter.getCategory())
                .map(Category::getId)
                .orElse(null);

        Long currentTopicId = topicRepository.findByName(newsletter.getTopic())
                .map(Topic::getId)
                .orElse(null);

        // 3. 현재 정보 DTO 조립
        InboxEditResponse.CurrentInfoDto currentInfo = InboxEditResponse.CurrentInfoDto.builder()
                .userNewsletterId(userNewsletter.getId())
                .categoryId(currentCategoryId)
                .topicId(currentTopicId)
                .memo(userNewsletter.getMemo())
                .build();

        // 4. 전체 선택지 목록 조회
        List<InboxEditResponse.CategoryDto> categories = categoryRepository.findAll().stream()
                .map(c -> new InboxEditResponse.CategoryDto(c.getId(), c.getName()))
                .collect(Collectors.toList());

        List<InboxEditResponse.TopicDto> topics = topicRepository.findAll().stream()
                .map(t -> new InboxEditResponse.TopicDto(t.getId(), t.getCategory().getId(), t.getName()))
                .collect(Collectors.toList());

        // 5. 최종 응답 조립
        return InboxEditResponse.builder()
                .current(currentInfo)
                .categories(categories)
                .topics(topics)
                .build();
    }

    @Transactional
    public void confirmAllInbox(Long userId) {
        userNewsletterRepository.bulkConfirmByUserId(
                userId,
                LocalDateTime.now(),
                LlmStatus.DONE
        );
    }

}
