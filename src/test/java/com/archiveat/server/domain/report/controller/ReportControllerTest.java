package com.archiveat.server.domain.report.controller;

import com.archiveat.server.domain.report.dto.response.*;
import com.archiveat.server.domain.report.service.ReportService;
import com.archiveat.server.global.jwt.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ReportControllerTest.TestConfig.class)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportService reportService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @TestConfiguration
    static class TestConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new HandlerMethodArgumentResolver() {
                @Override
                public boolean supportsParameter(MethodParameter parameter) {
                    return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
                }

                @Override
                public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                              NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                    return 1L; // 테스트 시 유저 ID 1L 고정
                }
            });
        }
    }

    @Test
    @DisplayName("[Report] 주간 리포트 메인 조회 API 검증")
    void getWeeklyReport_Success() throws Exception {
        // given
        WeeklyReportResponse response = new WeeklyReportResponse(
                "1월 첫째주", "AI 코멘트", 10, 5, 3, 2, 4, 1, List.of()
        );
        given(reportService.getWeeklyReport(1L)).willReturn(response);

        // when & then
        mockMvc.perform(get("/report"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.data.weekLabel").value("1월 첫째주"))
                .andDo(print());

        // 정확한 유저 ID가 서비스로 전달되었는지 확인합니다.
        verify(reportService).getWeeklyReport(eq(1L));
    }

    @Test
    @DisplayName("[Report] 핵심 소비현황 조회 API 검증")
    void getConsumption_Success() throws Exception {
        // given
        ConsumptionResponse response = new ConsumptionResponse(10, 5, List.of());
        given(reportService.getConsumption(1L)).willReturn(response);

        // when & then
        mockMvc.perform(get("/report/weekly/consumption"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalSavedCount").value(10));

        verify(reportService).getConsumption(eq(1L));
    }

    @Test
    @DisplayName("[Report] 나의 소비 밸런스 조회 API 검증")
    void getBalance_Success() throws Exception {
        // given
        BalanceResponse response = new BalanceResponse("제목", "설명", "문구", 3, 2, 4, 1);
        given(reportService.getBalance(1L)).willReturn(response);

        // when & then
        mockMvc.perform(get("/report/weekly/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.patternTitle").value("제목"));

        verify(reportService).getBalance(eq(1L));
    }

    @Test
    @DisplayName("[Report] 관심사 갭 분석 조회 API 검증")
    void getGapAnalysis_Success() throws Exception {
        // given
        GapAnalysisResponse response = new GapAnalysisResponse(List.of());
        given(reportService.getGapAnalysis(1L)).willReturn(response);

        // when & then
        mockMvc.perform(get("/report/weekly/gap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true));

        verify(reportService).getGapAnalysis(eq(1L));
    }
}