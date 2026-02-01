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
     * 주간 리포트 메인 조회
     * URL: GET /report
     */
    @GetMapping
    public ApiResponse<WeeklyReportResponse> getWeeklyReport(
            @AuthenticationPrincipal Long userId) {
        WeeklyReportResponse response = reportService.getWeeklyReport(userId);
        return ApiResponse.ok(response);
    }

    /**
     * 핵심 소비현황 조회
     * URL: GET /report/weekly/consumption
     */
    @GetMapping("/weekly/consumption") // [수정 1] 상위 경로가 있으므로 /consumption만 남김
    public ApiResponse<ConsumptionResponse> getConsumption(
            @AuthenticationPrincipal Long userId) {
        ConsumptionResponse response = reportService.getConsumption(userId);
        return ApiResponse.ok(response);
    }

    /**
     * 나의 소비 밸런스 조회
     * URL: GET /report/weekly/balance
     */
    @GetMapping("/weekly/balance")
    public ApiResponse<BalanceResponse> getBalance(
            @AuthenticationPrincipal Long userId) {
        BalanceResponse response = reportService.getBalance(userId);
        return ApiResponse.ok(response);
    }

    /**
     * 관심사 갭 분석 조회
     * URL: GET /report/weekly/gap
     */
    @GetMapping("/weekly/gap")
    public ApiResponse<GapAnalysisResponse> getGapAnalysis(
            @AuthenticationPrincipal Long userId) {
        GapAnalysisResponse response = reportService.getGapAnalysis(userId);
        return ApiResponse.ok(response);
    }


}