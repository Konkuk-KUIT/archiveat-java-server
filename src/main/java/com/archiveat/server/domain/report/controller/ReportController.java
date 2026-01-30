package com.archiveat.server.domain.report.controller;

import com.archiveat.server.domain.report.dto.response.*;
import com.archiveat.server.domain.report.service.ReportService;
import com.archiveat.server.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/report")
public class ReportController {

    private final ReportService reportService;

    /**
     * 주간 리포트 전체 조회
     * 
     * @param userId 인증된 사용자 ID
     */
    @GetMapping
    public ApiResponse<WeeklyReportResponse> getWeeklyReport(
            @AuthenticationPrincipal Long userId) {
        WeeklyReportResponse response = reportService.getWeeklyReport(userId);
        return ApiResponse.ok(response);
    }

    /**
     * 핵심 소비현황 조회
     * 
     * @param userId 인증된 사용자 ID
     */
    @GetMapping("/weekly/consumption")
    public ApiResponse<ConsumptionResponse> getConsumption(
            @AuthenticationPrincipal Long userId) {
        ConsumptionResponse response = reportService.getConsumption(userId);
        return ApiResponse.ok(response);
    }

    /**
     * 나의 소비 밸런스 조회
     * 
     * @param userId 인증된 사용자 ID
     */
    @GetMapping("/weekly/balance")
    public ApiResponse<BalanceResponse> getBalance(
            @AuthenticationPrincipal Long userId) {
        BalanceResponse response = reportService.getBalance(userId);
        return ApiResponse.ok(response);
    }

    /**
     * 관심사 갭 분석 조회
     * 
     * @param userId 인증된 사용자 ID
     */
    @GetMapping("/weekly/gap")
    public ApiResponse<GapAnalysisResponse> getGapAnalysis(
            @AuthenticationPrincipal Long userId) {
        GapAnalysisResponse response = reportService.getGapAnalysis(userId);
        return ApiResponse.ok(response);
    }
}
