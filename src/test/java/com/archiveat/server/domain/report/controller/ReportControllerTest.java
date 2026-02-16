package com.archiveat.server.domain.report.controller;

import com.archiveat.server.domain.report.dto.response.*;
import com.archiveat.server.domain.report.service.ReportService;
import com.archiveat.server.global.jwt.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any; // [Reason] anyLong 대신 any 사용
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportController.class)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportService reportService;

    @MockitoBean
    private JwtUtil jwtUtil; // [Insight] 보안 필터 구동을 위한 필수 부속품입니다.

    @Test
    @WithMockUser(username = "1")
    @DisplayName("[Report] 주간 리포트 메인 조회 API 검증")
    void getWeeklyReport_Success() throws Exception {
        // given
        WeeklyReportResponse response = new WeeklyReportResponse(
                "1월 첫째주", "AI 코멘트", 10, 5, 3, 2, 4, 1, List.of()
        );

        // [Insight] any()를 사용하여 타입 불일치(null 전달) 문제를 해결합니다.
        when(reportService.getWeeklyReport(any())).thenReturn(response);

        // when & then
        mockMvc.perform(get("/report"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.data.weekLabel").value("1월 첫째주"));
    }

    @Test
    @WithMockUser(username = "1")
    @DisplayName("[Report] 핵심 소비현황 조회 API 검증")
    void getConsumption_Success() throws Exception {
        // given
        ConsumptionResponse response = new ConsumptionResponse(10, 5, List.of());

        when(reportService.getConsumption(any())).thenReturn(response);

        // when & then
        mockMvc.perform(get("/report/weekly/consumption"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalSavedCount").value(10));
    }

    @Test
    @WithMockUser(username = "1")
    @DisplayName("[Report] 나의 소비 밸런스 조회 API 검증")
    void getBalance_Success() throws Exception {
        // given
        BalanceResponse response = new BalanceResponse("제목", "설명", "문구", 3, 2, 4, 1);

        when(reportService.getBalance(any())).thenReturn(response);

        // when & then
        mockMvc.perform(get("/report/weekly/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.patternTitle").value("제목"));
    }

    @Test
    @WithMockUser(username = "1")
    @DisplayName("[Report] 관심사 갭 분석 조회 API 검증")
    void getGapAnalysis_Success() throws Exception {
        // given
        GapAnalysisResponse response = new GapAnalysisResponse(List.of());

        when(reportService.getGapAnalysis(any())).thenReturn(response);

        // when & then
        mockMvc.perform(get("/report/weekly/gap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true));
    }
}