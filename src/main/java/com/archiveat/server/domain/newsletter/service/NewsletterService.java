package com.archiveat.server.domain.newsletter.service;

import com.archiveat.server.domain.newsletter.dto.response.*;
import com.archiveat.server.domain.newsletter.entity.Domain;
import com.archiveat.server.domain.newsletter.entity.Newsletter;
import com.archiveat.server.domain.newsletter.entity.UserNewsletter;
import com.archiveat.server.domain.newsletter.event.NewsletterProcessRequestedEvent;
import com.archiveat.server.domain.newsletter.repository.DomainRepository;
import com.archiveat.server.domain.newsletter.repository.NewsletterRepository;
import com.archiveat.server.domain.newsletter.repository.UserNewsletterRepository;
import com.archiveat.server.domain.user.entity.User;
import com.archiveat.server.domain.user.repository.UserRepository;
import com.archiveat.server.global.client.PythonClientService;
import com.archiveat.server.global.common.constant.LlmStatus;
import com.archiveat.server.global.util.DomainClassifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@RequiredArgsConstructor
@Service
public class NewsletterService {
    private final NewsletterRepository newsletterRepository;
    private final UserNewsletterRepository userNewsletterRepository;
    private final UserRepository userRepository;
    private final DomainRepository domainRepository;
    private final PythonClientService pythonClientService;
    private final com.archiveat.server.domain.explore.repository.UserTopicRepository userTopicRepository;

    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public DeleteNewsletterResponse deleteUserNewsletter(Long userId, Long userNewsletterId) {
        int deleted = userNewsletterRepository.deleteByIdAndUser_Id(userNewsletterId, userId);
        if (deleted == 0) {
            // TODO throw new NewsletterNotFoundException
        }
        return new DeleteNewsletterResponse(userNewsletterId);
    }

    @Transactional
    public ViewNewsletterResponse viewUserNewsletter(Long userId, Long userNewsletterId) {
        UserNewsletter userNewsletter = userNewsletterRepository
                .findByIdAndUser_Id(userNewsletterId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Newsletter not found or access denied"));

        if (!userNewsletter.isRead())
            userNewsletter.updateIsRead();
        else
            userNewsletter.updateLastViewedAt();
        userNewsletterRepository.save(userNewsletter);

        Newsletter newsletter = userNewsletter.getNewsletter();

        // newsletter_summary JSON 파싱
        List<NewsletterSummaryBlock> summaryBlocks = parseNewsletterSummary(newsletter.getNewsletterSummary());

        // Label 계산: UserNewsletter에 저장된 perspectiveType + depthType 조합
        String label = com.archiveat.server.domain.newsletter.util.LabelFormatter.formatLabel(
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
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(
                    newsletterSummaryJson,
                    mapper.getTypeFactory().constructCollectionType(List.class, NewsletterSummaryBlock.class));
        } catch (Exception e) {
            // 파싱 실패 시 빈 리스트 반환
            return List.of();
        }
    }

    @Transactional
    public SimpleViewNewsletterResponse simpleViewUserNewsletter(Long userId, Long userNewsletterId) {
        UserNewsletter userNewsletter = userNewsletterRepository
                .findByIdAndUser_Id(userNewsletterId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Newsletter not found or access denied"));

        if (!userNewsletter.isRead())
            userNewsletter.updateIsRead();
        else
            userNewsletter.updateLastViewedAt();
        userNewsletterRepository.save(userNewsletter);

        Newsletter newsletter = userNewsletter.getNewsletter();

        List<NewsletterSummaryBlock> summaryBlocks = List.of();

        // Label 계산
        String label = com.archiveat.server.domain.newsletter.util.LabelFormatter.formatLabel(
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
     * Newsletter 생성 엔드포인트 (비동기 패턴)
     * 
     * 1. Newsletter를 PENDING 상태로 DB에 저장
     * 2. 즉시 클라이언트에 응답 반환 (PENDING 상태)
     * 3. 백그라운드에서 비동기 작업 시작 (processNewsletterAsync)
     */
    @Transactional
    public GenerateNewsletterResponse generateNewsletter(Long userId, String contentUrl, String memo) {
        Domain domain = resolveDomainFromUrl(contentUrl);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User Not Found"));

        Newsletter newsletter = newsletterRepository.findByContentUrl(contentUrl)
                .orElseGet(() -> newsletterRepository.save(Newsletter.createPending(domain, contentUrl)));

        UserNewsletter userNewsletter = userNewsletterRepository.save(
                UserNewsletter.create(user, newsletter, memo));

        // 비동기 작업 시작 (트랜잭션 커밋 후 실행)
        // @Async 메서드는 별도 스레드에서 실행되므로 즉시 반환됩니다
        Long newsletterId = newsletter.getId();
        // processNewsletterAsync(newsletterId, contentUrl);
        applicationEventPublisher.publishEvent(new NewsletterProcessRequestedEvent(newsletterId, contentUrl));

        return new GenerateNewsletterResponse(
                userNewsletter.getId(),
                newsletter.getLlmStatus().name());
    }

    /**
     * Newsletter 비동기 처리 메서드
     * 
     * 백그라운드에서 실행되며, Python 서버 호출 및 DB 업데이트를 담당합니다.
     * 처리 시간: 5-10초 (YouTube 데이터 추출 + Gemini LLM 요약)
     */
    public void processNewsletterAsync(Long newsletterId, String contentUrl) {
        log.info("Starting async newsletter processing for ID: {}", newsletterId);
        long startTime = System.currentTimeMillis();

        try {
            // 1. Newsletter 상태를 RUNNING으로 업데이트
            Newsletter newsletter = newsletterRepository.findById(newsletterId)
                    .orElseThrow(() -> new IllegalArgumentException("Newsletter not found: " + newsletterId));
            newsletter.updateLlmStatus(LlmStatus.RUNNING);
            newsletterRepository.save(newsletter);
            log.info("Newsletter {} status updated to RUNNING", newsletterId);

            // 2. URL 도메인 자동 분류
            DomainClassifier.DomainType domainType = DomainClassifier.classify(contentUrl);
            log.info("URL classified as: {} - {}", domainType, domainType.getDescription());

            // 3. Python 서버 호출 (도메인 타입에 따라 적절한 엔드포인트 호출)
            CompletableFuture<PythonSummaryResponse> future;

            if (domainType.isYouTube()) {
                // YouTube 영상 처리
                future = pythonClientService.requestYouTubeSummary(contentUrl);
            } else if (domainType.needsWebCrawling()) {
                // 네이버 뉴스, 티스토리, 브런치, 일반 웹 크롤링
                // user memo는 UserNewsletter에서 가져와야 하지만,
                // 현재는 Newsletter만 전달받으므로 null 처리
                // TODO: 필요시 UserNewsletter의 memo도 함께 전달
                future = pythonClientService.requestNaverNewsSummary(contentUrl, null);
            } else {
                throw new IllegalArgumentException("Unsupported domain type: " + domainType);
            }

            PythonSummaryResponse response = future.get(10, TimeUnit.MINUTES); // 블로킹 대기 (백그라운드 스레드이므로 OK)

            // 4. Newsletter 업데이트 (DONE 상태)
            newsletter.updateFromPythonResponse(response);
            newsletterRepository.save(newsletter);

            // 5. 이 Newsletter를 사용하는 모든 UserNewsletter의 label 구성 요소 업데이트
            updateLabelComponentsForAllUsers(newsletter);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Newsletter {} processed successfully in {}ms", newsletterId, duration);

        } catch (Exception e) {
            // 에러 발생 시 FAILED 상태로 저장
            log.error("Failed to process newsletter {}: {}", newsletterId, e.getMessage(), e);

            try {
                Newsletter newsletter = newsletterRepository.findById(newsletterId).orElse(null);
                if (newsletter != null) {
                    String errorMsg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
                    newsletter.setErrorMessage(errorMsg);
                    newsletter.updateLlmStatus(LlmStatus.FAILED);
                    newsletterRepository.save(newsletter);
                }
            } catch (Exception saveError) {
                log.error("Failed to save error status for newsletter {}", newsletterId, saveError);
            }

            long duration = System.currentTimeMillis() - startTime;
            log.error("Newsletter {} processing failed after {}ms", newsletterId, duration);
        }
    }

    @Transactional
    public void updateIsRead(Long userId, Long userNewsletterId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User Not Found"));

        UserNewsletter userNewsletter = userNewsletterRepository.findByIdAndUser_Id(userNewsletterId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Newsletter not found or access denied"));

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
        if (host.contains("news.naver.com")) {
            return "Naver News";
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
            com.archiveat.server.global.common.constant.PerspectiveType perspectiveType = calculatePerspectiveType(
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
    private com.archiveat.server.global.common.constant.DepthType calculateDepthType(Integer consumptionTimeMin) {
        if (consumptionTimeMin == null) {
            return null;
        }
        return consumptionTimeMin < 10
                ? com.archiveat.server.global.common.constant.DepthType.LIGHT
                : com.archiveat.server.global.common.constant.DepthType.DEEP;
    }

    /**
     * 사용자의 NOW 관심사 카테고리 포함 여부로 PerspectiveType 계산
     */
    private com.archiveat.server.global.common.constant.PerspectiveType calculatePerspectiveType(Long userId,
            String categoryName) {
        if (categoryName == null) {
            return null;
        }

        List<String> nowCategories = userTopicRepository.findCategoryNamesByUserIdAndPerspectiveType(
                userId,
                com.archiveat.server.global.common.constant.PerspectiveType.NOW);

        return nowCategories.contains(categoryName)
                ? com.archiveat.server.global.common.constant.PerspectiveType.NOW
                : com.archiveat.server.global.common.constant.PerspectiveType.FUTURE;
    }
}
