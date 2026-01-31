package com.archiveat.server.domain.report.dto.response;

import java.util.List;

public record WeeklyReportResponse(
        String weekLabel, // "1월 첫째주"
        String aiComment, // AI 피드백 (하드코딩)
        Integer totalSavedCount, // 총 저장 개수
        Integer totalReadCount, // 총 읽음 개수
        Integer lightCount, // Light 개수
        Integer deepCount, // Deep 개수
        Integer nowCount, // Now 개수
        Integer futureCount, // Future 개수
        List<InterestGap> interestGaps // 관심사 갭 분석
) {
    public record InterestGap(
            String topicName,
            Integer savedCount,
            Integer readCount) {
    }
}
