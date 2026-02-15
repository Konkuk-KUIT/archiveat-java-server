package com.archiveat.server.domain.newsletter.service;

import com.archiveat.server.domain.explore.entity.Topic;
import com.archiveat.server.domain.explore.entity.TopicNewsletter;
import com.archiveat.server.domain.explore.repository.TopicNewsletterRepository;
import com.archiveat.server.domain.explore.repository.TopicRepository;
import com.archiveat.server.domain.newsletter.dto.response.*;
import com.archiveat.server.domain.newsletter.entity.Domain;
import com.archiveat.server.domain.newsletter.entity.Newsletter;
import com.archiveat.server.domain.newsletter.entity.UserNewsletter;
import com.archiveat.server.domain.newsletter.event.NewsletterProcessRequestedEvent;
import com.archiveat.server.domain.newsletter.repository.DomainRepository;
import com.archiveat.server.domain.newsletter.repository.NewsletterRepository;
import com.archiveat.server.domain.newsletter.repository.UserNewsletterRepository;
import com.archiveat.server.domain.newsletter.util.LabelFormatter;
import com.archiveat.server.domain.explore.repository.UserTopicRepository;
import com.archiveat.server.domain.user.entity.User;
import com.archiveat.server.domain.user.repository.UserRepository;
import com.archiveat.server.global.client.PythonClientService;
import org.springframework.dao.DataIntegrityViolationException;
// import com.archiveat.server.global.client.dto.PythonSummaryResponse;
import com.archiveat.server.global.common.constant.LlmStatus;
import com.archiveat.server.global.common.response.ErrorCode;
import com.archiveat.server.global.exception.CustomException;
import com.archiveat.server.global.lock.DistributedLockService;
import com.archiveat.server.global.security.TokenHashUtil;
import com.archiveat.server.global.util.DomainClassifier;
import com.archiveat.server.global.util.UrlNormalizer;
import com.archiveat.server.global.util.UrlRedirectResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.archiveat.server.global.common.constant.DepthType;
import com.archiveat.server.global.common.constant.PerspectiveType;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@RequiredArgsConstructor
@Service
public class NewsletterService {

    private final ObjectMapper objectMapper; // Bean 주입

    private final NewsletterRepository newsletterRepository;
    private final UserNewsletterRepository userNewsletterRepository;
    private final UserRepository userRepository;
    private final DomainRepository domainRepository;
    private final PythonClientService pythonClientService;
    private final UserTopicRepository userTopicRepository;
    private final NewsletterSynchronizer newsletterSynchronizer; // 동시성 제어용 컴포넌트
    private final TopicNewsletterRepository topicNewsletterRepository;
    private final TopicRepository topicRepository;

    private final ApplicationEventPublisher applicationEventPublisher;
    private final DistributedLockService distributedLockService;
    private final TokenHashUtil tokenHashUtil;
    private final CacheManager cacheManager;
    private final UrlRedirectResolver urlRedirectResolver;

    private final TransactionTemplate transactionTemplate;

    @Transactional
    public DeleteNewsletterResponse deleteUserNewsletter(Long userId, Long userNewsletterId) {
        int deleted = userNewsletterRepository.deleteByIdAndUser_Id(userNewsletterId, userId);
        if (deleted == 0) {
            throw new CustomException(ErrorCode.USER_NEWSLETTER_NOT_FOUND);
        }
        return new DeleteNewsletterResponse(userNewsletterId);
    }

    /**
     * 뉴스레터 AI 요약 상세 조회
     */
    @Transactional
    public ViewNewsletterResponse viewUserNewsletter(Long userId, Long userNewsletterId, boolean autoMarkRead) {
        UserNewsletter userNewsletter = userNewsletterRepository
                .findByIdAndUser_Id(userNewsletterId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NEWSLETTER_NOT_FOUND));

        if (autoMarkRead) {
            if (!userNewsletter.isRead())
                userNewsletter.updateIsRead();
            else
                userNewsletter.updateLastViewedAt();
        } else {
            userNewsletter.updateLastViewedAt();
        }
        userNewsletterRepository.save(userNewsletter);

        Newsletter newsletter = userNewsletter.getNewsletter();
        List<NewsletterSummaryBlock> summaryBlocks = parseNewsletterSummary(newsletter.getNewsletterSummary());

        String label = LabelFormatter.formatLabel(
                userNewsletter.getDepthType(),
                userNewsletter.getPerspectiveType());

        return new ViewNewsletterResponse(
                userNewsletter.getId(),
                newsletter.getCategory(),
                newsletter.getTopic(),
                newsletter.getTitle(),
                newsletter.getThumbnailUrl(),
                label,
                userNewsletter.getMemo(),
                newsletter.getContentUrl(),
                summaryBlocks);
    }

    private List<NewsletterSummaryBlock> parseNewsletterSummary(String newsletterSummaryJson) {
        if (newsletterSummaryJson == null || newsletterSummaryJson.isEmpty() || newsletterSummaryJson.equals("[]")) {
            return List.of();
        }
        try {
            return objectMapper.readValue(
                    newsletterSummaryJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, NewsletterSummaryBlock.class));
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * 뉴스레터 Simple 상세 조회
     */
    @Transactional
    public SimpleViewNewsletterResponse simpleViewUserNewsletter(Long userId, Long userNewsletterId,
            boolean autoMarkRead) {
        UserNewsletter userNewsletter = userNewsletterRepository
                .findByIdAndUser_Id(userNewsletterId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NEWSLETTER_NOT_FOUND));

        if (autoMarkRead) {
            if (!userNewsletter.isRead())
                userNewsletter.updateIsRead();
            else
                userNewsletter.updateLastViewedAt();
        } else {
            userNewsletter.updateLastViewedAt();
        }
        userNewsletterRepository.save(userNewsletter);

        Newsletter newsletter = userNewsletter.getNewsletter();
        List<NewsletterSummaryBlock> allSummaryBlocks = parseNewsletterSummary(newsletter.getNewsletterSummary());
        List<NewsletterSummaryBlock> summaryBlocks = allSummaryBlocks.isEmpty() ? List.of()
                : List.of(allSummaryBlocks.get(0));

        String label = LabelFormatter.formatLabel(
                userNewsletter.getDepthType(),
                userNewsletter.getPerspectiveType());

        return new SimpleViewNewsletterResponse(
                userNewsletter.getId(),
                newsletter.getCategory(),
                newsletter.getTopic(),
                newsletter.getTitle(),
                newsletter.getThumbnailUrl(),
                label,
                userNewsletter.getMemo(),
                newsletter.getContentUrl(),
                summaryBlocks);
    }

    /**
     * Newsletter 생성 엔드포인트
     */
    public GenerateNewsletterResponse generateNewsletter(Long userId, String contentUrl, String memo) {
        String firstNormalized = UrlNormalizer.normalize(contentUrl);
        String resolvedUrl = urlRedirectResolver.resolveIfShortUrl(firstNormalized);
        String normalizedUrl = UrlNormalizer.normalize(resolvedUrl);
        log.info("Normalized URL: {}", normalizedUrl);

        GenerateNewsletterResponse response = transactionTemplate.execute(status -> {
            String domainName = normalizeDomainName(extractDomainName(normalizedUrl));
            Domain domain = newsletterSynchronizer.getOrCreateDomain(domainName);

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

            // [Concurrency Fix] 별도 트랜잭션으로 처리
            Newsletter newsletter = newsletterSynchronizer.getOrCreatePendingNewsletter(domain, normalizedUrl);

            if (userNewsletterRepository.existsByUserAndNewsletter(user, newsletter)) {
                throw new CustomException(ErrorCode.NEWSLETTER_ALREADY_EXISTS);
            }

            UserNewsletter userNewsletter = UserNewsletter.create(user, newsletter, memo);

            // 이미 분석 완료된(DONE) 뉴스레터라면 바로 라벨 계산
            if (newsletter.getLlmStatus() == LlmStatus.DONE) {
                DepthType depthType = calculateDepthType(newsletter.getConsumptionTimeMin());
                // 단건 조회용 메서드 사용
                PerspectiveType perspectiveType = calculatePerspectiveTypeSingle(userId, newsletter.getCategory());

                userNewsletter.updateLabelComponents(perspectiveType, depthType);
                try {
                    userNewsletterRepository.save(userNewsletter);
                } catch (DataIntegrityViolationException e) {
                    throw new CustomException(ErrorCode.NEWSLETTER_ALREADY_EXISTS);
                }
                log.info("Newsletter {} is already DONE. Calculated labels synchronously for user {}", newsletter.getId(),
                        userId);
            } else {
                // PENDING 상태라면 저장 후 이벤트 발행
                try {
                    userNewsletterRepository.save(userNewsletter);
                } catch (DataIntegrityViolationException e) {
                    throw new CustomException(ErrorCode.NEWSLETTER_ALREADY_EXISTS);
                }
                Long newsletterId = newsletter.getId();
                applicationEventPublisher.publishEvent(new NewsletterProcessRequestedEvent(newsletterId, normalizedUrl));
                log.info("Newsletter event published: newsletterId={}", newsletterId);
            }

            return new GenerateNewsletterResponse(
                    userNewsletter.getId(),
                    newsletter.getLlmStatus().name());
        });

        if (response == null) {
            throw new CustomException(ErrorCode.INTERNAL_ERROR);
        }
        return response;
    }

    /**
     * Newsletter 비동기 처리 메서드
     */
    public void processNewsletterAsync(Long newsletterId, String contentUrl) {
        log.info("Starting async newsletter processing for ID: {}", newsletterId);
        long startTime = System.currentTimeMillis();
        String lockKey = "newsletter:lock:" + tokenHashUtil.sha256Hex(contentUrl);

        if (!distributedLockService.tryLock(lockKey, 1)) {
            log.warn("Failed to acquire lock for newsletter {}, another process may be working on it", newsletterId);
            return;
        }

        try {
            Newsletter newsletter = newsletterRepository.findById(newsletterId)
                    .orElseThrow(() -> new CustomException(ErrorCode.NEWSLETTER_NOT_FOUND));

            if (newsletter.getLlmStatus() == LlmStatus.DONE) {
                log.info("Newsletter {} already processed, skipping", newsletterId);
                return;
            }

            newsletter.updateLlmStatus(LlmStatus.RUNNING);
            newsletterRepository.save(newsletter);

            DomainClassifier.DomainType domainType = DomainClassifier.classify(contentUrl);
            CompletableFuture<PythonSummaryResponse> future;

            if (domainType.isYouTube()) {
                future = pythonClientService.requestYouTubeSummary(contentUrl);
            } else if (domainType.isTistory()) {
                future = pythonClientService.requestTistorySummary(contentUrl, null);
            } else if (domainType.needsWebCrawling()) {
                future = pythonClientService.requestNaverNewsSummary(contentUrl, null);
            } else {
                throw new CustomException(ErrorCode.UNSUPPORTED_DOMAIN_TYPE, "Unsupported domain type: " + domainType);
            }

            PythonSummaryResponse response;
            try {
                response = future.get(10, TimeUnit.MINUTES);
            } catch (Exception e) {
                log.error("Python server execution failed: {}", e.getMessage());
                throw new CustomException(ErrorCode.CRAWLING_FAILED);
            }

            // 4. Newsletter 업데이트 (DONE 상태)
            saveNewsletterWithTopic(newsletter, response);

            // 5. 캐시 무효화 (Stale Cache 방지) ⭐
            evictNewsletterCache(newsletterId, contentUrl);

            // [Performance Fix] N+1 문제 해결된 Bulk Update 호출
            updateLabelComponentsForAllUsers(newsletter);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Newsletter {} processed successfully in {}ms", newsletterId, duration);

        } catch (CustomException ce) {
            log.warn("CustomException occurred: {}", ce.getMessage());
            markNewsletterFailed(newsletterId, contentUrl, ce.getMessage());
            throw ce;
        } catch (Exception e) {
            log.error("Failed to process newsletter {}", newsletterId, e);
            markNewsletterFailed(newsletterId, contentUrl, e.getMessage());
            throw new RuntimeException("Newsletter processing failed", e);
        } finally {
            distributedLockService.unlock(lockKey);
        }
    }

    /*
     * Newsletter 저장을 별도의 transaction으로 구성 -> 불일치 문제 해결
     * Spring @Transactional 은 프록시 패턴으로 동작
     * 클라이언트 호출
     * ↓
     * NewsletterService 프록시 (트랜잭션 시작)
     * ↓
     * 실제 NewsletterService 객체
     * 
     * 하지만 같은 클래스 내에서 호출하면:
     * processNewsletterAsync() 내부에서
     * ↓
     * this.saveNewsletterWithTopic() 호출
     * ↓
     * 프록시를 거치지 않음 @Transactional 무시
     * 
     * @Transactional 대신 transaction Template 사용하여 해결
     */
    protected void saveNewsletterWithTopic(Newsletter newsletter, PythonSummaryResponse response) {
        transactionTemplate.executeWithoutResult(status -> {
            if (response.getAnalysis() == null) {
                throw new CustomException(ErrorCode.INVALID_PYTHON_RESPONSE,
                        "Analysis is null in response");
            }

            newsletter.updateFromPythonResponse(response);
            newsletterRepository.save(newsletter);

            Topic topic = topicRepository
                    .findByName(response.getAnalysis().getTopicName())
                    .orElseThrow(() -> new CustomException(ErrorCode.TOPIC_NOT_FOUND));

            topicNewsletterRepository.save(new TopicNewsletter(topic, newsletter));
        });
    }

    @Transactional
    protected void markNewsletterFailed(Long newsletterId, String contentUrl, String errorMessage) {
        try {
            Newsletter newsletter = newsletterRepository.findById(newsletterId).orElse(null);
            if (newsletter != null) {
                newsletter.setErrorMessage(errorMessage);
                newsletter.updateLlmStatus(LlmStatus.FAILED);
                newsletterRepository.save(newsletter);
                evictNewsletterCache(newsletterId, contentUrl);
            }
        } catch (Exception saveError) {
            log.error("Failed to save error status for newsletter {}", newsletterId, saveError);
        }
    }

    @Transactional
    public void updateIsRead(Long userId, Long userNewsletterId) {
        UserNewsletter userNewsletter = userNewsletterRepository.findByIdAndUser_Id(userNewsletterId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NEWSLETTER_NOT_FOUND));
        userNewsletter.updateIsRead();
    }

    // --- Helper Methods ---

    private String extractDomainName(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (host == null)
                return null;
            if (host.startsWith("www."))
                host = host.substring(4);
            return host;
        } catch (Exception e) {
            return null;
        }
    }

    private String normalizeDomainName(String host) {
        if (host == null)
            return "Unknown";
        if (host.contains("youtube.com") || host.contains("youtu.be"))
            return "YouTube";
        if (host.contains("news.naver.com"))
            return "Naver News";
        if (host.contains("brunch.co.kr"))
            return "Brunch";
        if (host.contains("naver.com"))
            return "Naver";
        if (host.contains("tistory.com"))
            return "tistory";
        return host;
    }

    // --- Label Calculation (Bulk Optimized) ---

    private void updateLabelComponentsForAllUsers(Newsletter newsletter) {
        List<UserNewsletter> userNewsletters = userNewsletterRepository.findAllByNewsletter_Id(newsletter.getId());
        if (userNewsletters.isEmpty())
            return;

        DepthType depthType = calculateDepthType(newsletter.getConsumptionTimeMin());

        // 1. User IDs 추출
        List<Long> userIds = userNewsletters.stream()
                .map(un -> un.getUser().getId())
                .distinct()
                .collect(Collectors.toList());

        // 2. Bulk Fetch (N+1 문제 해결)
        List<Object[]> userTopics = userTopicRepository.findCategoryNamesByUserIdsAndPerspectiveType(userIds,
                PerspectiveType.NOW);

        // 3. Map 변환 (UserId -> List<CategoryName>)
        Map<Long, List<String>> userTopicMap = userTopics.stream()
                .collect(Collectors.groupingBy(
                        row -> (Long) row[0],
                        Collectors.mapping(row -> (String) row[1], Collectors.toList())));

        // 4. Update Loop (DB 조회 없이 메모리에서 처리)
        for (UserNewsletter userNewsletter : userNewsletters) {
            Long userId = userNewsletter.getUser().getId();
            List<String> nowCategories = userTopicMap.getOrDefault(userId, List.of());

            PerspectiveType perspectiveType = calculatePerspectiveTypeWithCache(nowCategories,
                    newsletter.getCategory());
            userNewsletter.updateLabelComponents(perspectiveType, depthType);
            userNewsletterRepository.save(userNewsletter);
        }
    }

    private DepthType calculateDepthType(Integer consumptionTimeMin) {
        if (consumptionTimeMin == null)
            return null;
        return consumptionTimeMin < 10 ? DepthType.LIGHT : DepthType.DEEP;
    }

    // 단건 처리용 (DB 조회 발생)
    private PerspectiveType calculatePerspectiveTypeSingle(Long userId, String categoryName) {
        if (categoryName == null)
            return null;
        List<String> nowCategories = userTopicRepository.findCategoryNamesByUserIdAndPerspectiveType(userId,
                PerspectiveType.NOW);
        return calculatePerspectiveTypeWithCache(nowCategories, categoryName);
    }

    // 캐시된(이미 조회된) 리스트 사용
    private PerspectiveType calculatePerspectiveTypeWithCache(List<String> nowCategories, String categoryName) {
        if (categoryName == null)
            return null;
        return nowCategories.contains(categoryName) ? PerspectiveType.NOW : PerspectiveType.FUTURE;
    }

    private void evictNewsletterCache(Long newsletterId, String contentUrl) {
        try {
            if (cacheManager.getCache("newsletter") != null) {
                cacheManager.getCache("newsletter").evict(newsletterId);
                cacheManager.getCache("newsletter").evict("url::" + contentUrl);
                log.debug("Evicted newsletter cache: id={}, url={}", newsletterId, contentUrl);
            }
        } catch (Exception e) {
            log.warn("Failed to evict cache", e);
        }
    }
}
