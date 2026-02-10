package com.archiveat.server.domain.newsletter.service;

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
import com.archiveat.server.global.common.constant.LlmStatus;
import com.archiveat.server.global.common.response.ErrorCode;
import com.archiveat.server.global.exception.CustomException;
import com.archiveat.server.global.lock.DistributedLockService;
import com.archiveat.server.global.security.TokenHashUtil;
import com.archiveat.server.global.util.DomainClassifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.archiveat.server.global.common.constant.DepthType;
import com.archiveat.server.global.common.constant.PerspectiveType;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@RequiredArgsConstructor
@Service
public class NewsletterService {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final NewsletterRepository newsletterRepository;
    private final UserNewsletterRepository userNewsletterRepository;
    private final UserRepository userRepository;
    private final DomainRepository domainRepository;
    private final PythonClientService pythonClientService;
    private final UserTopicRepository userTopicRepository;

    private final ApplicationEventPublisher applicationEventPublisher; // Event 발행
    private final DistributedLockService distributedLockService;
    private final TokenHashUtil tokenHashUtil;
    private final CacheManager cacheManager;

    @Transactional
    public DeleteNewsletterResponse deleteUserNewsletter(Long userId, Long userNewsletterId) {
        int deleted = userNewsletterRepository.deleteByIdAndUser_Id(userNewsletterId, userId);
        if (deleted == 0) {
            // 보안: 존재 여부와 권한 여부를 구분하지 않고 404로 통일
            throw new CustomException(ErrorCode.USER_NEWSLETTER_NOT_FOUND);
        }
        return new DeleteNewsletterResponse(userNewsletterId);
    }

    @Transactional
    public ViewNewsletterResponse viewUserNewsletter(Long userId, Long userNewsletterId) {
        UserNewsletter userNewsletter = userNewsletterRepository
                .findByIdAndUser_Id(userNewsletterId, userId)
                .orElseThrow(() -> new com.archiveat.server.global.exception.CustomException(
                        com.archiveat.server.global.common.response.ErrorCode.USER_NEWSLETTER_NOT_FOUND));

        if (!userNewsletter.isRead())
            userNewsletter.updateIsRead();
        else
            userNewsletter.updateLastViewedAt();
        userNewsletterRepository.save(userNewsletter);

        Newsletter newsletter = userNewsletter.getNewsletter();

        // newsletter_summary JSON 파싱
        List<NewsletterSummaryBlock> summaryBlocks = parseNewsletterSummary(newsletter.getNewsletterSummary());

        // Label 계산: UserNewsletter에 저장된 perspectiveType + depthType 조합
        String label = LabelFormatter.formatLabel(
                userNewsletter.getDepthType(),
                userNewsletter.getPerspectiveType());

        return new ViewNewsletterResponse(
                userNewsletter.getId(), // userNewsletterId
                newsletter.getCategory(), // categoryName
                newsletter.getTopic(), // topicName
                newsletter.getTitle(),
                newsletter.getThumbnailUrl(),
                label,
                userNewsletter.getMemo(),
                newsletter.getContentUrl(),
                summaryBlocks);
    }

    /**
     * JSON 문자열을 NewsletterSummaryBlock 리스트로 파싱
     */
    private List<NewsletterSummaryBlock> parseNewsletterSummary(String newsletterSummaryJson) {
        if (newsletterSummaryJson == null || newsletterSummaryJson.isEmpty() || newsletterSummaryJson.equals("[]")) {
            return List.of();
        }

        try {
            return objectMapper.readValue(
                    newsletterSummaryJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, NewsletterSummaryBlock.class));
        } catch (Exception e) {
            // 파싱 실패 시 빈 리스트 반환
            return List.of();
        }
    }

    @Transactional
    public SimpleViewNewsletterResponse simpleViewUserNewsletter(Long userId, Long userNewsletterId) {
        UserNewsletter userNewsletter = userNewsletterRepository
                .findByIdAndUser_Id(userNewsletterId, userId)
                .orElseThrow(() -> new com.archiveat.server.global.exception.CustomException(
                        com.archiveat.server.global.common.response.ErrorCode.USER_NEWSLETTER_NOT_FOUND));

        if (!userNewsletter.isRead())
            userNewsletter.updateIsRead();
        else
            userNewsletter.updateLastViewedAt();
        userNewsletterRepository.save(userNewsletter);

        Newsletter newsletter = userNewsletter.getNewsletter();

        List<NewsletterSummaryBlock> summaryBlocks = List.of();

        // Label 계산
        String label = LabelFormatter.formatLabel(
                userNewsletter.getDepthType(),
                userNewsletter.getPerspectiveType());

        return new SimpleViewNewsletterResponse(
                userNewsletter.getId(), // userNewsletterId
                newsletter.getCategory(), // categoryName
                newsletter.getTopic(), // topicName
                newsletter.getTitle(),
                newsletter.getThumbnailUrl(),
                label,
                userNewsletter.getMemo(),
                newsletter.getContentUrl(),
                summaryBlocks);
    }

    /**
     * Newsletter 생성 엔드포인트 (Event 기반 비동기 패턴)
     * 
     * 1. Newsletter를 PENDING 상태로 DB에 저장
     * 2. 이벤트 발행 (트랜잭션 커밋 후 EventListener가 Redis Queue에 추가)
     * 3. 즉시 클라이언트에 응답 반환 (PENDING 상태)
     * 4. Worker가 큐에서 작업을 가져가 비동기 처리
     * 
     * 트랜잭션 문제 해결:
     * - @Transactional 안에서 직접 큐에 넣으면 커밋 전까지 Redis에 반영 안 됨
     * - Event + @TransactionalEventListener(AFTER_COMMIT)로 분리하여 해결
     */
    @Transactional
    public GenerateNewsletterResponse generateNewsletter(Long userId, String contentUrl, String memo) {
        Domain domain = resolveDomainFromUrl(contentUrl);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 중복 URL 체크: 해당 사용자가 이미 이 URL을 저장했는지 확인
        newsletterRepository.findByContentUrl(contentUrl)
                .ifPresent(existingNewsletter -> {
                    boolean alreadyExists = userNewsletterRepository
                            .existsByUserAndNewsletter(user, existingNewsletter);
                    if (alreadyExists) {
                        throw new CustomException(ErrorCode.NEWSLETTER_ALREADY_EXISTS);
                    }
                });

        Newsletter newsletter = newsletterRepository.findByContentUrl(contentUrl)
                .orElseGet(() -> newsletterRepository.save(Newsletter.createPending(domain, contentUrl)));

        UserNewsletter userNewsletter = userNewsletterRepository.save(
                UserNewsletter.create(user, newsletter, memo));

        // 이벤트 발행 (EventListener가 트랜잭션 커밋 후 큐에 추가)
        Long newsletterId = newsletter.getId();
        applicationEventPublisher.publishEvent(
                new NewsletterProcessRequestedEvent(newsletterId, contentUrl));
        log.info("Newsletter event published: newsletterId={}", newsletterId);

        return new GenerateNewsletterResponse(
                userNewsletter.getId(),
                newsletter.getLlmStatus().name());
    }

    /**
     * Newsletter 비동기 처리 메서드 (분산 락 + 캐시 무효화 적용)
     * 
     * Worker에서 호출되며, Python 서버 호출 및 DB 업데이트를 담당합니다.
     * 처리 시간: 5-10초 (YouTube 데이터 추출 + Gemini LLM 요약)
     */
    public void processNewsletterAsync(Long newsletterId, String contentUrl) {
        log.info("Starting async newsletter processing for ID: {}", newsletterId);
        long startTime = System.currentTimeMillis();

        // 분산 락 키 생성 (SHA-256 해시로 충돌 방지)
        String lockKey = "newsletter:lock:" + tokenHashUtil.sha256Hex(contentUrl);

        // 분산 락 획득 시도 (Watchdog 활성화, leaseTime = -1)
        if (!distributedLockService.tryLock(lockKey, 1)) {
            log.warn("Failed to acquire lock for newsletter {}, another process may be working on it", newsletterId);
            return; // 다른 프로세스가 이미 처리 중이므로 종료
        }

        try {
            // 1. Newsletter 상태를 RUNNING으로 업데이트
            Newsletter newsletter = newsletterRepository.findById(newsletterId)
                    .orElseThrow(() -> new CustomException(ErrorCode.NEWSLETTER_NOT_FOUND));

            // 이미 처리 완료된 경우 스킵
            if (newsletter.getLlmStatus() == LlmStatus.DONE) {
                log.info("Newsletter {} already processed, skipping", newsletterId);
                return;
            }

            newsletter.updateLlmStatus(LlmStatus.RUNNING);
            newsletterRepository.save(newsletter);
            log.info("Newsletter {} status updated to RUNNING", newsletterId);

            // 2. URL 도메인 자동 분류
            DomainClassifier.DomainType domainType = DomainClassifier.classify(contentUrl);
            log.info("URL classified as: {} - {}", domainType, domainType.getDescription());

            // 3. Python 서버 호출 (도메인 타입에 따라 적절한 엔드포인트 호출)
            CompletableFuture<PythonSummaryResponse> future;

            if (domainType.isYouTube()) {
                future = pythonClientService.requestYouTubeSummary(contentUrl);
            } else if (domainType.isTistory()) {
                future = pythonClientService.requestTistorySummary(contentUrl, null);
            } else if (domainType.needsWebCrawling()) {
                future = pythonClientService.requestNaverNewsSummary(contentUrl, null);
            } else {
                throw new CustomException(
                        ErrorCode.UNSUPPORTED_DOMAIN_TYPE,
                        "Unsupported domain type: " + domainType);
            }

            // Python 서버 호출 및 크롤링 실패 처리
            PythonSummaryResponse response;
            try {
                response = future.get(10, TimeUnit.MINUTES);
            } catch (java.util.concurrent.TimeoutException e) {
                throw new CustomException(ErrorCode.CRAWLING_FAILED);
            } catch (java.util.concurrent.ExecutionException e) {
                log.error("Python server execution failed: {}", e.getCause().getMessage(), e);
                throw new CustomException(ErrorCode.CRAWLING_FAILED);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new CustomException(ErrorCode.CRAWLING_FAILED);
            }

            // 4. Newsletter 업데이트 (DONE 상태)
            newsletter.updateFromPythonResponse(response);
            newsletterRepository.save(newsletter);

            // 5. 캐시 무효화 (Stale Cache 방지) ⭐
            evictNewsletterCache(newsletterId, contentUrl);

            // 6. 이 Newsletter를 사용하는 모든 UserNewsletter의 label 구성 요소 업데이트
            updateLabelComponentsForAllUsers(newsletter);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Newsletter {} processed successfully in {}ms", newsletterId, duration);

        } catch (CustomException ce) {
            // CustomException은 재시도/DLQ 처리를 위해 그대로 재throw
            log.warn("CustomException occurred while processing newsletter {}: {}", newsletterId, ce.getMessage());
            markNewsletterFailed(newsletterId, contentUrl, ce.getMessage());
            throw ce;

        } catch (Exception e) {
            // 에러 발생 시 FAILED 상태로 저장
            log.error("Failed to process newsletter {}: {}", newsletterId, e.getMessage(), e);

            String errorMsg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            markNewsletterFailed(newsletterId, contentUrl, errorMsg);

            long duration = System.currentTimeMillis() - startTime;
            log.error("Newsletter {} processing failed after {}ms", newsletterId, duration);

            // 예외를 다시 던져서 Worker가 재시도/DLQ 처리할 수 있게 함
            throw new RuntimeException("Newsletter processing failed", e);

        } finally {
            // 분산 락 해제
            distributedLockService.unlock(lockKey);
        }
    }

    /**
     * Newsletter 처리 실패 시 상태를 FAILED로 저장하고 캐시 무효화
     */
    @Transactional
    private void markNewsletterFailed(Long newsletterId, String contentUrl, String errorMessage) {
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

    private Domain getOrCreateDomain(String domainName) {
        return domainRepository.findByName(domainName)
                .orElseGet(() -> {
                    try {
                        return domainRepository.save(new Domain(domainName));
                    } catch (DataIntegrityViolationException e) {
                        // 동시성 조절: 동시에 다른 트랜잭션에서 생성한 경우
                        return domainRepository.findByName(domainName)
                                .orElseThrow(() -> e);
                    }
                });
    }

    private Domain resolveDomainFromUrl(String contentUrl) {
        String host = extractDomainName(contentUrl);
        String domainName = normalizeDomainName(host);
        return getOrCreateDomain(domainName);
    }

    public String extractDomainName(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost(); // www.youtube.com
            if (host == null)
                return null;

            // www 제거
            if (host.startsWith("www.")) {
                host = host.substring(4);
            }
            return host; // youtube.com
        } catch (Exception e) {
            return null;
        }
    }

    public String normalizeDomainName(String host) {
        if (host == null)
            return "Unknown";

        if (host.contains("youtube.com") || host.contains("youtu.be")) {
            return "YouTube";
        }
        if (host.contains("news.naver.com")) {
            return "Naver News";
        }
        if (host.contains("brunch.co.kr")) {
            return "Brunch";
        }
        if (host.contains("naver.com")) {
            return "Naver";
        }
        if (host.contains("tistory.com")) {
            return "tistory";
        }
        return host; // fallback
    }

    /**
     * Newsletter의 label 구성 요소(perspectiveType, depthType)를
     * 모든 UserNewsletter에 대해 계산하여 업데이트
     */
    private void updateLabelComponentsForAllUsers(Newsletter newsletter) {
        // 이 Newsletter를 사용하는 모든 UserNewsletter 조회
        List<UserNewsletter> userNewsletters = userNewsletterRepository.findAllByNewsletter_Id(newsletter.getId());

        for (UserNewsletter userNewsletter : userNewsletters) {
            Long userId = userNewsletter.getUser().getId();

            // 1. DepthType 계산 (소비 시간 기준)
            com.archiveat.server.global.common.constant.DepthType depthType = calculateDepthType(
                    newsletter.getConsumptionTimeMin());

            // 2. PerspectiveType 계산 (사용자의 NOW 관심사 카테고리 확인)
            PerspectiveType perspectiveType = calculatePerspectiveType(
                    userId,
                    newsletter.getCategory());

            // 3. UserNewsletter 업데이트
            userNewsletter.updateLabelComponents(perspectiveType, depthType);
            userNewsletterRepository.save(userNewsletter);
        }
    }

    /**
     * 소비 시간 기준으로 DepthType 계산
     */
    private DepthType calculateDepthType(Integer consumptionTimeMin) {
        if (consumptionTimeMin == null) {
            return null;
        }
        return consumptionTimeMin < 10
                ? DepthType.LIGHT
                : DepthType.DEEP;
    }

    /**
     * 사용자의 NOW 관심사 카테고리 포함 여부로 PerspectiveType 계산
     */
    private PerspectiveType calculatePerspectiveType(Long userId,
            String categoryName) {
        if (categoryName == null) {
            return null;
        }

        List<String> nowCategories = userTopicRepository.findCategoryNamesByUserIdAndPerspectiveType(
                userId,
                PerspectiveType.NOW);

        return nowCategories.contains(categoryName)
                ? PerspectiveType.NOW
                : PerspectiveType.FUTURE;
    }

    /**
     * Newsletter 캐시 무효화
     * Python 작업 완료 후 stale cache 방지
     */
    private void evictNewsletterCache(Long newsletterId, String contentUrl) {
        try {
            if (cacheManager.getCache("newsletter") != null) {
                cacheManager.getCache("newsletter").evict(newsletterId);
                cacheManager.getCache("newsletter").evict("url::" + contentUrl);
                log.debug("Evicted newsletter cache: id={}, url={}", newsletterId, contentUrl);
            }
        } catch (Exception e) {
            log.warn("Failed to evict cache for newsletter {}", newsletterId, e);
        }
    }
}
