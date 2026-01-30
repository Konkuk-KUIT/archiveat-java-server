package com.archiveat.server.domain.report.service;

import com.archiveat.server.domain.explore.entity.Category;
import com.archiveat.server.domain.explore.entity.Topic;
import com.archiveat.server.domain.explore.entity.TopicNewsletter;
import com.archiveat.server.domain.explore.repository.TopicNewsletterRepository;
import com.archiveat.server.domain.newsletter.entity.UserNewsletter;
import com.archiveat.server.domain.newsletter.repository.UserNewsletterRepository;
import com.archiveat.server.domain.report.dto.response.*;
import com.archiveat.server.global.common.constant.DepthType;
import com.archiveat.server.global.common.constant.PerspectiveType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final UserNewsletterRepository userNewsletterRepository;
    private final TopicNewsletterRepository topicNewsletterRepository;

    /**
     * 주간 리포트 전체 정보 조회
     */
    @Transactional(readOnly = true)
    public WeeklyReportResponse getWeeklyReport(Long userId) {
        LocalDateTime[] weekRange = getCurrentWeekRange();
        LocalDateTime weekStart = weekRange[0];
        LocalDateTime weekEnd = weekRange[1];

        // 1. 저장/읽음 개수 집계
        List<UserNewsletter> savedThisWeek = userNewsletterRepository.findByUserIdAndCreatedAtBetween(userId, weekStart,
                weekEnd);
        List<UserNewsletter> readThisWeek = userNewsletterRepository
                .findByUserIdAndLastViewedAtBetweenAndIsReadTrue(userId, weekStart, weekEnd);

        int totalSavedCount = savedThisWeek.size();
        int totalReadCount = readThisWeek.size();

        // 2. 밸런스 집계
        Map<String, Integer> balance = calculateBalance(readThisWeek);

        // 3. 관심사 갭 분석
        List<WeeklyReportResponse.InterestGap> interestGaps = calculateInterestGaps(userId, weekStart, weekEnd);

        // 4. 주차 라벨 생성
        String weekLabel = generateWeekLabel(weekStart);

        // 5. AI 코멘트 (하드코딩)
        String aiComment = "편식 없는 지식 섭취가 필요해요! IT 트렌드는 잘 따라가고 있지만, 경제 분야는 놓치고 있어요.";

        return new WeeklyReportResponse(
                weekLabel,
                aiComment,
                totalSavedCount,
                totalReadCount,
                balance.get("light"),
                balance.get("deep"),
                balance.get("now"),
                balance.get("future"),
                interestGaps);
    }

    /**
     * 핵심 소비현황 조회
     */
    @Transactional(readOnly = true)
    public ConsumptionResponse getConsumption(Long userId) {
        LocalDateTime[] weekRange = getCurrentWeekRange();
        LocalDateTime weekStart = weekRange[0];
        LocalDateTime weekEnd = weekRange[1];

        List<UserNewsletter> savedThisWeek = userNewsletterRepository.findByUserIdAndCreatedAtBetween(userId, weekStart,
                weekEnd);
        List<UserNewsletter> readThisWeek = userNewsletterRepository
                .findByUserIdAndLastViewedAtBetweenAndIsReadTrue(userId, weekStart, weekEnd);

        // 최근 읽은 뉴스레터 목록
        List<UserNewsletter> recentReadList = userNewsletterRepository
                .findByUserIdAndIsReadTrueOrderByLastViewedAtDesc(userId);

        List<ConsumptionResponse.RecentRead> recentReads = recentReadList.stream()
                .map(un -> {
                    String categoryName = getCategoryNameFromNewsletter(un);
                    LocalDate lastViewedDate = un.getLastViewedAt() != null
                            ? un.getLastViewedAt().toLocalDate()
                            : LocalDate.now();

                    return new ConsumptionResponse.RecentRead(
                            un.getNewsletter().getId(),
                            un.getNewsletter().getTitle(),
                            categoryName,
                            lastViewedDate);
                })
                .collect(Collectors.toList());

        return new ConsumptionResponse(
                savedThisWeek.size(),
                readThisWeek.size(),
                recentReads);
    }

    /**
     * 소비 밸런스 조회
     */
    @Transactional(readOnly = true)
    public BalanceResponse getBalance(Long userId) {
        LocalDateTime[] weekRange = getCurrentWeekRange();
        List<UserNewsletter> readThisWeek = userNewsletterRepository
                .findByUserIdAndLastViewedAtBetweenAndIsReadTrue(userId, weekRange[0], weekRange[1]);

        Map<String, Integer> balance = calculateBalance(readThisWeek);

        // 패턴 메시지 생성
        Map<String, String> pattern = generatePatternMessages(balance);

        return new BalanceResponse(
                pattern.get("title"),
                pattern.get("description"),
                pattern.get("quote"),
                balance.get("light"),
                balance.get("deep"),
                balance.get("now"),
                balance.get("future"));
    }

    /**
     * 관심사 갭 분석 조회
     */
    @Transactional(readOnly = true)
    public GapAnalysisResponse getGapAnalysis(Long userId) {
        LocalDateTime[] weekRange = getCurrentWeekRange();
        List<WeeklyReportResponse.InterestGap> gaps = calculateInterestGaps(userId, weekRange[0], weekRange[1]);

        // InterestGap을 TopicGap으로 변환 (ID 포함)
        List<GapAnalysisResponse.TopicGap> topicGaps = gaps.stream()
                .map(gap -> new GapAnalysisResponse.TopicGap(
                        null, // Topic ID는 추후 매핑 필요
                        gap.topicName(),
                        gap.savedCount(),
                        gap.readCount()))
                .limit(4)
                .collect(Collectors.toList());

        return new GapAnalysisResponse(topicGaps);
    }

    // ============== Private Helper Methods ==============

    /**
     * 현재 주의 시작(월요일 00:00)과 종료(일요일 23:59) 반환
     */
    private LocalDateTime[] getCurrentWeekRange() {
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        LocalDateTime weekStart = monday.atStartOfDay();
        LocalDateTime weekEnd = sunday.atTime(23, 59, 59);

        return new LocalDateTime[] { weekStart, weekEnd };
    }

    /**
     * Light/Deep, Now/Future 밸런스 집계
     */
    private Map<String, Integer> calculateBalance(List<UserNewsletter> newsletters) {
        int lightCount = 0, deepCount = 0, nowCount = 0, futureCount = 0;

        for (UserNewsletter un : newsletters) {
            if (un.getDepthType() == DepthType.LIGHT)
                lightCount++;
            if (un.getDepthType() == DepthType.DEEP)
                deepCount++;
            if (un.getPerspectiveType() == PerspectiveType.NOW)
                nowCount++;
            if (un.getPerspectiveType() == PerspectiveType.FUTURE)
                futureCount++;
        }

        Map<String, Integer> balance = new HashMap<>();
        balance.put("light", lightCount);
        balance.put("deep", deepCount);
        balance.put("now", nowCount);
        balance.put("future", futureCount);

        return balance;
    }

    /**
     * 관심사 갭 분석: |저장 - 읽음| 절댓값이 큰 순서로 Top 4
     */
    private List<WeeklyReportResponse.InterestGap> calculateInterestGaps(Long userId, LocalDateTime weekStart,
            LocalDateTime weekEnd) {
        List<UserNewsletter> savedThisWeek = userNewsletterRepository.findByUserIdAndCreatedAtBetween(userId, weekStart,
                weekEnd);
        List<UserNewsletter> readThisWeek = userNewsletterRepository
                .findByUserIdAndLastViewedAtBetweenAndIsReadTrue(userId, weekStart, weekEnd);

        // Newsletter ID 수집
        List<Long> savedNewsletterIds = savedThisWeek.stream()
                .map(un -> un.getNewsletter().getId())
                .collect(Collectors.toList());

        List<Long> readNewsletterIds = readThisWeek.stream()
                .map(un -> un.getNewsletter().getId())
                .collect(Collectors.toList());

        // 모든 Newsletter ID 합치기
        Set<Long> allNewsletterIds = new HashSet<>();
        allNewsletterIds.addAll(savedNewsletterIds);
        allNewsletterIds.addAll(readNewsletterIds);

        if (allNewsletterIds.isEmpty()) {
            return Collections.emptyList();
        }

        // TopicNewsletter로 Newsletter → Topic 매핑
        List<TopicNewsletter> topicNewsletters = topicNewsletterRepository
                .findByNewsletterIdIn(new ArrayList<>(allNewsletterIds));

        // Topic별 저장/읽음 개수 집계
        Map<String, Integer> topicSavedCount = new HashMap<>();
        Map<String, Integer> topicReadCount = new HashMap<>();

        for (TopicNewsletter tn : topicNewsletters) {
            Topic topic = tn.getTopic();
            if (topic == null)
                continue;

            Category category = topic.getCategory();
            String topicName = category != null ? category.getName() : topic.getName();

            Long newsletterId = tn.getNewsletter().getId();

            // 저장 개수
            if (savedNewsletterIds.contains(newsletterId)) {
                topicSavedCount.put(topicName, topicSavedCount.getOrDefault(topicName, 0) + 1);
            }

            // 읽음 개수
            if (readNewsletterIds.contains(newsletterId)) {
                topicReadCount.put(topicName, topicReadCount.getOrDefault(topicName, 0) + 1);
            }
        }

        // 모든 토픽명 수집
        Set<String> allTopics = new HashSet<>();
        allTopics.addAll(topicSavedCount.keySet());
        allTopics.addAll(topicReadCount.keySet());

        // Gap 계산 및 정렬
        return allTopics.stream()
                .map(topicName -> {
                    int saved = topicSavedCount.getOrDefault(topicName, 0);
                    int read = topicReadCount.getOrDefault(topicName, 0);
                    return new WeeklyReportResponse.InterestGap(topicName, saved, read);
                })
                .sorted((a, b) -> {
                    int gapA = Math.abs(a.savedCount() - a.readCount());
                    int gapB = Math.abs(b.savedCount() - b.readCount());
                    return Integer.compare(gapB, gapA); // 내림차순
                })
                .limit(4)
                .collect(Collectors.toList());
    }

    /**
     * 주차 라벨 생성: "1월 첫째주", "1월 둘째주" 등
     */
    private String generateWeekLabel(LocalDateTime weekStart) {
        int month = weekStart.getMonthValue();
        int weekOfMonth = (weekStart.getDayOfMonth() - 1) / 7 + 1;

        String[] weekNames = { "첫째주", "둘째주", "셋째주", "넷째주", "다섯째주" };
        String weekName = weekOfMonth <= 5 ? weekNames[weekOfMonth - 1] : "다섯째주";

        return month + "월 " + weekName;
    }

    /**
     * 패턴 메시지 생성 (샘플 메시지)
     */
    private Map<String, String> generatePatternMessages(Map<String, Integer> balance) {
        int light = balance.get("light");
        int deep = balance.get("deep");
        int now = balance.get("now");
        int future = balance.get("future");

        Map<String, String> pattern = new HashMap<>();

        // Light 위주
        if (light > deep) {
            pattern.put("title", "핵심을 빠르게 파악하는 당신");
            pattern.put("description", "10분 미만의 가볍고 빠른 콘텐츠를 선호하시네요!");
            pattern.put("quote", "빠르고 효율적인 학습이 강점입니다.");
        } else {
            pattern.put("title", "깊이 있는 통찰을 추구하는 당신");
            pattern.put("description", "심도 있는 긴 콘텐츠를 즐기시는군요!");
            pattern.put("quote", "깊이 있는 사고가 당신의 무기입니다.");
        }

        return pattern;
    }

    /**
     * Newsletter로부터 Category 이름 추출
     */
    private String getCategoryNameFromNewsletter(UserNewsletter un) {
        // TopicNewsletter를 통해 Topic → Category 매핑
        List<TopicNewsletter> topicNewsletters = topicNewsletterRepository.findByNewsletterIdIn(
                Collections.singletonList(un.getNewsletter().getId()));

        if (!topicNewsletters.isEmpty()) {
            Topic topic = topicNewsletters.get(0).getTopic();
            if (topic != null && topic.getCategory() != null) {
                return topic.getCategory().getName();
            }
        }

        return "기타";
    }
}
