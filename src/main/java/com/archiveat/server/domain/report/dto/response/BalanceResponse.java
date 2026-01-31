package com.archiveat.server.domain.report.dto.response;

public record BalanceResponse(
        String patternTitle, // "핵심을 빠르게 파악하는..."
        String patternDescription, // "10분 미만의..."
        String patternQuote, // "현재에 충실한..."
        Integer lightCount,
        Integer deepCount,
        Integer nowCount,
        Integer futureCount) {
}
