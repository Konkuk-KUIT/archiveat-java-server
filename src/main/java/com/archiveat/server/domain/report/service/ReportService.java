package com.archiveat.server.domain.report.service;

import com.archiveat.server.domain.newsletter.entity.UserNewsletter;
import com.archiveat.server.domain.newsletter.repository.UserNewsletterRepository;
import com.archiveat.server.domain.report.dto.response.*;
import com.archiveat.server.global.common.constant.DepthType;
import com.archiveat.server.global.common.constant.PerspectiveType;
import com.archiveat.server.global.common.constant.DateTimeConstant;
// Removed unused imports
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime; // 추가됨
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final UserNewsletterRepository userNewsletterRepository;

    /**
     * 주간 리포트 전체 정보 조회
     */
    @Transactional(readOnly = true)
    public WeeklyReportResponse getWeeklyReport(Long userId) {
        LocalDateTime[] weekRange = getCurrentWeekRange();
        LocalDateTime weekStart = weekRange[0];
        LocalDateTime weekEnd = weekRange[1];

        // 1. 저장/읽음 개수 집계 (여기서 한 번만 조회)
        List<UserNewsletter> savedThisWeek = userNewsletterRepository.findByUserIdAndCreatedAtBetween(userId, weekStart,
                weekEnd);
        List<UserNewsletter> readThisWeek = userNewsletterRepository
                .findByUserIdAndLastViewedAtBetweenAndIsReadTrue(userId, weekStart, weekEnd);

        int totalSavedCount = savedThisWeek.size();
        int totalReadCount = readThisWeek.size();

        // 2. 밸런스 집계 (저장한 것 기준)
        Map<String, Integer> balance = calculateBalance(savedThisWeek);

        // 3. 관심사 갭 분석 (수정: 조회한 리스트를 파라미터로 전달하여 중복 쿼리 방지)
        List<WeeklyReportResponse.InterestGap> interestGaps = calculateInterestGaps(savedThisWeek, readThisWeek);

        // 4. 주차 라벨 생성 (UTC -> KST 변환 후 생성)
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
        List<UserNewsletter> savedThisWeek = userNewsletterRepository.findByUserIdAndCreatedAtBetween(userId,
                weekRange[0], weekRange[1]);
        List<UserNewsletter> readThisWeek = userNewsletterRepository
                .findByUserIdAndLastViewedAtBetweenAndIsReadTrue(userId, weekRange[0], weekRange[1]);

        // 최근 읽은 뉴스레터 목록
        List<UserNewsletter> recentReadList = userNewsletterRepository
                .findByUserIdAndIsReadTrueOrderByLastViewedAtDesc(userId);

        // Newsletter.category 필드를 직접 사용 (TopicNewsletter 불필요)
        List<ConsumptionResponse.RecentRead> recentReads = recentReadList.stream()
                .map(un -> {
                    // NPE 방지: Newsletter가 null인 경우 처리
                    String categoryName = (un.getNewsletter() != null && un.getNewsletter().getCategory() != null)
                            ? un.getNewsletter().getCategory()
                            : "기타";

                    LocalDate lastViewedDate = un.getLastViewedAt() != null
                            ? un.getLastViewedAt().atZone(ZoneId.of("UTC"))
                                    .withZoneSameInstant(DateTimeConstant.APP_ZONE).toLocalDate()
                            : LocalDate.now(DateTimeConstant.APP_ZONE);

                    return new ConsumptionResponse.RecentRead(
                            un.getNewsletter() != null ? un.getNewsletter().getId() : 0L,
                            un.getNewsletter() != null ? un.getNewsletter().getTitle() : "삭제된 뉴스레터",
                            categoryName,
                            lastViewedDate);
                }).collect(Collectors.toList());

        return new ConsumptionResponse(savedThisWeek.size(), readThisWeek.size(), recentReads);

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
        List<UserNewsletter> savedThisWeek = userNewsletterRepository.findByUserIdAndCreatedAtBetween(userId,
                weekRange[0], weekRange[1]);
        List<UserNewsletter> readThisWeek = userNewsletterRepository
                .findByUserIdAndLastViewedAtBetweenAndIsReadTrue(userId, weekRange[0], weekRange[1]);

        List<WeeklyReportResponse.InterestGap> gaps = calculateInterestGaps(savedThisWeek, readThisWeek);

        // InterestGap을 TopicGap으로 변환
        List<GapAnalysisResponse.TopicGap> topicGaps = gaps.stream()
                .map(gap -> new GapAnalysisResponse.TopicGap(
                        gap.topicId(),
                        gap.topicName(),
                        gap.savedCount(),
                        gap.readCount()))
                .limit(4)
                .collect(Collectors.toList());

        return new GapAnalysisResponse(topicGaps);
    }

    // ============== Private Helper Methods ==============

    /**
     * 현재 주의 시작(월요일 00:00 KST)과 종료(일요일 23:59:59... KST)를 UTC로 변환하여 반환
     */
    private LocalDateTime[] getCurrentWeekRange() {
        // 1. KST 기준 현재 날짜
        LocalDate todayKst = LocalDate.now(DateTimeConstant.APP_ZONE);

        // 2. KST 기준 월요일, 일요일 계산
        LocalDate mondayKst = todayKst.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sundayKst = todayKst.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        // 3. KST 기준 시작/종료 시간
        LocalDateTime kstStart = mondayKst.atStartOfDay();
        LocalDateTime kstEnd = sundayKst.atTime(LocalTime.MAX);

        // 4. UTC로 변환 (DB 조회용)
        // DB에는 UTC로 저장되어 있다고 가정하므로, KST 범위를 UTC 범위로 변환하여 조회
        LocalDateTime utcStart = kstStart.atZone(DateTimeConstant.APP_ZONE)
                .withZoneSameInstant(ZoneId.of("UTC"))
                .toLocalDateTime();
        LocalDateTime utcEnd = kstEnd.atZone(DateTimeConstant.APP_ZONE)
                .withZoneSameInstant(ZoneId.of("UTC"))
                .toLocalDateTime();

        return new LocalDateTime[] { utcStart, utcEnd };
    }

    /**
     * 주차 라벨 생성 (입력받은 weekStart는 UTC이므로 KST로 변환 후 사용)
     */
    private String generateWeekLabel(LocalDateTime weekStartUtc) {
        // UTC -> KST 변환
        LocalDateTime kstStart = weekStartUtc.atZone(ZoneId.of("UTC"))
                .withZoneSameInstant(DateTimeConstant.APP_ZONE)
                .toLocalDateTime();

        int month = kstStart.getMonthValue();

        // 월의 몇 번째 주인지 계산 (W 방식)
        // 예: 2월 2주차 -> 2월 둘째주
        int weekOfMonth = (kstStart.getDayOfMonth() - 1) / 7 + 1;

        String[] weekNames = { "첫째주", "둘째주", "셋째주", "넷째주", "다섯째주" };
        String weekName = weekOfMonth <= 5 ? weekNames[weekOfMonth - 1] : "다섯째주";

        return month + "월 " + weekName;
    }

    /**
     * Light/Deep, Now/Future 밸런스 집계
     */
    private Map<String, Integer> calculateBalance(List<UserNewsletter> newsletters) {
        // ... (기존 로직 동일)
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
     * UserNewsletter.topic (Entity) 사용 (ID 포함)
     */
    private List<WeeklyReportResponse.InterestGap> calculateInterestGaps(
            List<UserNewsletter> savedThisWeek,
            List<UserNewsletter> readThisWeek) {

        // 1. Topic별 saved/read 카운트 및 이름 집계
        Map<Long, String> topicNames = new HashMap<>();
        Map<Long, Integer> savedCounts = new HashMap<>();
        Map<Long, Integer> readCounts = new HashMap<>();

        // Saved 집계
        for (UserNewsletter un : savedThisWeek) {
            if (un.getTopic() != null) {
                Long topicId = un.getTopic().getId();
                topicNames.putIfAbsent(topicId, un.getTopic().getName());
                savedCounts.put(topicId, savedCounts.getOrDefault(topicId, 0) + 1);
            }
        }

        // Read 집계
        for (UserNewsletter un : readThisWeek) {
            if (un.getTopic() != null) {
                Long topicId = un.getTopic().getId();
                topicNames.putIfAbsent(topicId, un.getTopic().getName());
                readCounts.put(topicId, readCounts.getOrDefault(topicId, 0) + 1);
            }
        }

        // 2. 결과 생성 및 정렬
        return topicNames.keySet().stream()
                .map(id -> new WeeklyReportResponse.InterestGap(
                        id,
                        topicNames.get(id),
                        savedCounts.getOrDefault(id, 0),
                        readCounts.getOrDefault(id, 0)))
                .sorted((a, b) -> {
                    int absGapA = Math.abs(a.savedCount() - a.readCount());
                    int absGapB = Math.abs(b.savedCount() - b.readCount());
                    return Integer.compare(absGapB, absGapA);
                })
                .limit(4)
                .collect(Collectors.toList());
    }

    // generateWeekLabel, generatePatternMessages는 기존과 동일하여 생략 가능하지만
    // 전체 코드의 완결성을 위해 아래에 유지합니다.

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