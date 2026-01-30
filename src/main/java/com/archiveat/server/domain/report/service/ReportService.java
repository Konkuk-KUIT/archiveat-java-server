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
import java.time.LocalTime; // 추가됨
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

        // 1. 저장/읽음 개수 집계 (여기서 한 번만 조회)
        List<UserNewsletter> savedThisWeek = userNewsletterRepository.findByUserIdAndCreatedAtBetween(userId, weekStart, weekEnd);
        List<UserNewsletter> readThisWeek = userNewsletterRepository.findByUserIdAndLastViewedAtBetweenAndIsReadTrue(userId, weekStart, weekEnd);

        int totalSavedCount = savedThisWeek.size();
        int totalReadCount = readThisWeek.size();

        // 2. 밸런스 집계
        Map<String, Integer> balance = calculateBalance(readThisWeek);

        // 3. 관심사 갭 분석 (수정: 조회한 리스트를 파라미터로 전달하여 중복 쿼리 방지)
        List<WeeklyReportResponse.InterestGap> interestGaps = calculateInterestGaps(savedThisWeek, readThisWeek);

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
     * 핵심 소비현황 조회 (N+1 문제 해결 적용)
     */
    @Transactional(readOnly = true)
    public ConsumptionResponse getConsumption(Long userId) {
        LocalDateTime[] weekRange = getCurrentWeekRange();
        List<UserNewsletter> savedThisWeek = userNewsletterRepository.findByUserIdAndCreatedAtBetween(userId, weekRange[0], weekRange[1]);
        List<UserNewsletter> readThisWeek = userNewsletterRepository.findByUserIdAndLastViewedAtBetweenAndIsReadTrue(userId, weekRange[0], weekRange[1]);

        // 1. 최근 읽은 뉴스레터 목록 (Repository 메서드명 변경 가정: findTop10...)
        // 만약 Repository 이름을 안 바꿨다면 기존 메서드를 쓰되, 아래 로직은 동일하게 적용
        List<UserNewsletter> recentReadList = userNewsletterRepository
                .findByUserIdAndIsReadTrueOrderByLastViewedAtDesc(userId);

        // [N+1 해결] 2. 뉴스레터 ID 목록 추출
        List<Long> newsletterIds = recentReadList.stream()
                .map(un -> un.getNewsletter().getId())
                .toList();

        // [N+1 해결] 3. 관련된 토픽 정보를 한 번에 조회하여 Map으로 변환
        Map<Long, String> categoryMap = new HashMap<>();
        if (!newsletterIds.isEmpty()) {
            List<TopicNewsletter> topicNewsletters = topicNewsletterRepository.findByNewsletterIdIn(newsletterIds);

            for (TopicNewsletter tn : topicNewsletters) {
                // Topic이 없거나 Category가 없는 경우 대비
                String categoryName = "기타";
                if (tn.getTopic() != null && tn.getTopic().getCategory() != null) {
                    categoryName = tn.getTopic().getCategory().getName();
                }
                categoryMap.put(tn.getNewsletter().getId(), categoryName);
            }
        }

        // 4. Map을 사용하여 데이터 매핑 (DB 재조회 없음)
        List<ConsumptionResponse.RecentRead> recentReads = recentReadList.stream()
                .map(un -> {
                    String categoryName = categoryMap.getOrDefault(un.getNewsletter().getId(), "기타");

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

        // 수정: calculateInterestGaps가 리스트를 받도록 변경되었으므로 여기서 조회 후 전달
        List<UserNewsletter> savedThisWeek = userNewsletterRepository.findByUserIdAndCreatedAtBetween(userId, weekRange[0], weekRange[1]);
        List<UserNewsletter> readThisWeek = userNewsletterRepository.findByUserIdAndLastViewedAtBetweenAndIsReadTrue(userId, weekRange[0], weekRange[1]);

        List<WeeklyReportResponse.InterestGap> gaps = calculateInterestGaps(savedThisWeek, readThisWeek);

        // InterestGap을 TopicGap으로 변환
        List<GapAnalysisResponse.TopicGap> topicGaps = gaps.stream()
                .map(gap -> new GapAnalysisResponse.TopicGap(
                        null, // TODO: Topic ID 매핑 필요 시 로직 추가 필요
                        gap.topicName(),
                        gap.savedCount(),
                        gap.readCount()))
                .limit(4)
                .collect(Collectors.toList());

        return new GapAnalysisResponse(topicGaps);
    }

    // ============== Private Helper Methods ==============

    /**
     * 현재 주의 시작(월요일 00:00)과 종료(일요일 23:59:59.999...) 반환
     */
    private LocalDateTime[] getCurrentWeekRange() {
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        LocalDateTime weekStart = monday.atStartOfDay();
        // 수정: 23:59:59 대신 LocalTime.MAX 사용하여 정밀도 향상
        LocalDateTime weekEnd = sunday.atTime(LocalTime.MAX);

        return new LocalDateTime[] { weekStart, weekEnd };
    }

    /**
     * Light/Deep, Now/Future 밸런스 집계
     */
    private Map<String, Integer> calculateBalance(List<UserNewsletter> newsletters) {
        // ... (기존 로직 동일)
        int lightCount = 0, deepCount = 0, nowCount = 0, futureCount = 0;

        for (UserNewsletter un : newsletters) {
            if (un.getDepthType() == DepthType.LIGHT) lightCount++;
            if (un.getDepthType() == DepthType.DEEP) deepCount++;
            if (un.getPerspectiveType() == PerspectiveType.NOW) nowCount++;
            if (un.getPerspectiveType() == PerspectiveType.FUTURE) futureCount++;
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
     * 수정: DB 조회를 제거하고 파라미터로 리스트를 받음
     */
    private List<WeeklyReportResponse.InterestGap> calculateInterestGaps(
            List<UserNewsletter> savedThisWeek,
            List<UserNewsletter> readThisWeek) {

        // 1. Newsletter ID 수집
        Set<Long> savedNewsletterIds = savedThisWeek.stream()
                .map(un -> un.getNewsletter().getId())
                .collect(Collectors.toSet()); // 검색 속도를 위해 Set 사용

        Set<Long> readNewsletterIds = readThisWeek.stream()
                .map(un -> un.getNewsletter().getId())
                .collect(Collectors.toSet());

        // 2. 모든 Newsletter ID 합치기
        Set<Long> allNewsletterIds = new HashSet<>();
        allNewsletterIds.addAll(savedNewsletterIds);
        allNewsletterIds.addAll(readNewsletterIds);

        if (allNewsletterIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. TopicNewsletter로 Newsletter → Topic 매핑 (Bulk 조회)
        List<TopicNewsletter> topicNewsletters = topicNewsletterRepository
                .findByNewsletterIdIn(new ArrayList<>(allNewsletterIds));

        // 4. Topic별 저장/읽음 개수 집계
        Map<String, Integer> topicSavedCount = new HashMap<>();
        Map<String, Integer> topicReadCount = new HashMap<>();

        for (TopicNewsletter tn : topicNewsletters) {
            if (tn.getTopic() == null) continue;

            Category category = tn.getTopic().getCategory();
            String topicName = (category != null) ? category.getName() : tn.getTopic().getName();
            Long newsletterId = tn.getNewsletter().getId();

            if (savedNewsletterIds.contains(newsletterId)) {
                topicSavedCount.put(topicName, topicSavedCount.getOrDefault(topicName, 0) + 1);
            }
            if (readNewsletterIds.contains(newsletterId)) {
                topicReadCount.put(topicName, topicReadCount.getOrDefault(topicName, 0) + 1);
            }
        }

        // 5. Gap 계산 및 정렬 (기존 로직 동일)
        Set<String> allTopics = new HashSet<>();
        allTopics.addAll(topicSavedCount.keySet());
        allTopics.addAll(topicReadCount.keySet());

        return allTopics.stream()
                .map(topicName -> {
                    int saved = topicSavedCount.getOrDefault(topicName, 0);
                    int read = topicReadCount.getOrDefault(topicName, 0);
                    return new WeeklyReportResponse.InterestGap(topicName, saved, read);
                })
                .sorted((a, b) -> {
                    int gapA = Math.abs(a.savedCount() - a.readCount());
                    int gapB = Math.abs(b.savedCount() - b.readCount());
                    return Integer.compare(gapB, gapA);
                })
                .limit(4)
                .collect(Collectors.toList());
    }

    // generateWeekLabel, generatePatternMessages는 기존과 동일하여 생략 가능하지만
    // 전체 코드의 완결성을 위해 아래에 유지합니다.

    private String generateWeekLabel(LocalDateTime weekStart) {
        int month = weekStart.getMonthValue();
        int weekOfMonth = (weekStart.getDayOfMonth() - 1) / 7 + 1;
        String[] weekNames = { "첫째주", "둘째주", "셋째주", "넷째주", "다섯째주" };
        String weekName = weekOfMonth <= 5 ? weekNames[weekOfMonth - 1] : "다섯째주";
        return month + "월 " + weekName;
    }

    private Map<String, String> generatePatternMessages(Map<String, Integer> balance) {
        int light = balance.get("light");
        int deep = balance.get("deep");
        // ... (나머지 로직 동일)
        Map<String, String> pattern = new HashMap<>();
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
}