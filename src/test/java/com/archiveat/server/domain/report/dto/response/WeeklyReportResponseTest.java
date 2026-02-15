package com.archiveat.server.domain.report.dto.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WeeklyReportResponseTest {

    @Test
    @DisplayName("WeeklyReportResponse 생성 및 필드 접근 테스트")
    void createWeeklyReportResponse() {
        // given
        String weekLabel = "2월 첫째주";
        String aiComment = "편식 없는 지식 섭취가 필요해요!";
        Integer totalSavedCount = 10;
        Integer totalReadCount = 7;
        Integer lightCount = 5;
        Integer deepCount = 2;
        Integer nowCount = 4;
        Integer futureCount = 3;
        List<WeeklyReportResponse.InterestGap> interestGaps = new ArrayList<>();

        // when
        WeeklyReportResponse response = new WeeklyReportResponse(
                weekLabel,
                aiComment,
                totalSavedCount,
                totalReadCount,
                lightCount,
                deepCount,
                nowCount,
                futureCount,
                interestGaps
        );

        // then
        assertThat(response).isNotNull();
        assertThat(response.weekLabel()).isEqualTo(weekLabel);
        assertThat(response.aiComment()).isEqualTo(aiComment);
        assertThat(response.totalSavedCount()).isEqualTo(totalSavedCount);
        assertThat(response.totalReadCount()).isEqualTo(totalReadCount);
        assertThat(response.lightCount()).isEqualTo(lightCount);
        assertThat(response.deepCount()).isEqualTo(deepCount);
        assertThat(response.nowCount()).isEqualTo(nowCount);
        assertThat(response.futureCount()).isEqualTo(futureCount);
        assertThat(response.interestGaps()).isEqualTo(interestGaps);
    }

    @Test
    @DisplayName("InterestGap 중첩 레코드 생성 및 필드 접근 테스트")
    void createInterestGap() {
        // given
        Long topicId = 1L;
        String topicName = "Technology";
        Integer savedCount = 10;
        Integer readCount = 5;

        // when
        WeeklyReportResponse.InterestGap interestGap = new WeeklyReportResponse.InterestGap(
                topicId,
                topicName,
                savedCount,
                readCount
        );

        // then
        assertThat(interestGap).isNotNull();
        assertThat(interestGap.topicId()).isEqualTo(topicId);
        assertThat(interestGap.topicName()).isEqualTo(topicName);
        assertThat(interestGap.savedCount()).isEqualTo(savedCount);
        assertThat(interestGap.readCount()).isEqualTo(readCount);
    }

    @Test
    @DisplayName("InterestGap 리스트를 포함한 WeeklyReportResponse 생성 테스트")
    void createWeeklyReportResponseWithInterestGaps() {
        // given
        List<WeeklyReportResponse.InterestGap> interestGaps = List.of(
                new WeeklyReportResponse.InterestGap(1L, "Technology", 15, 10),
                new WeeklyReportResponse.InterestGap(2L, "Economy", 8, 2),
                new WeeklyReportResponse.InterestGap(3L, "Science", 5, 5),
                new WeeklyReportResponse.InterestGap(4L, "Culture", 12, 8)
        );

        // when
        WeeklyReportResponse response = new WeeklyReportResponse(
                "1월 둘째주",
                "AI 코멘트 테스트",
                40,
                25,
                20,
                20,
                25,
                15,
                interestGaps
        );

        // then
        assertThat(response.interestGaps()).hasSize(4);
        assertThat(response.interestGaps().get(0).topicName()).isEqualTo("Technology");
        assertThat(response.interestGaps().get(1).savedCount()).isEqualTo(8);
        assertThat(response.interestGaps().get(2).readCount()).isEqualTo(5);
        assertThat(response.interestGaps().get(3).topicId()).isEqualTo(4L);
    }

    @Test
    @DisplayName("카운트 필드가 0인 경우 테스트")
    void createWeeklyReportResponseWithZeroCounts() {
        // given & when
        WeeklyReportResponse response = new WeeklyReportResponse(
                "3월 셋째주",
                "아직 저장한 뉴스레터가 없어요.",
                0,
                0,
                0,
                0,
                0,
                0,
                List.of()
        );

        // then
        assertThat(response.totalSavedCount()).isEqualTo(0);
        assertThat(response.totalReadCount()).isEqualTo(0);
        assertThat(response.lightCount()).isEqualTo(0);
        assertThat(response.deepCount()).isEqualTo(0);
        assertThat(response.nowCount()).isEqualTo(0);
        assertThat(response.futureCount()).isEqualTo(0);
        assertThat(response.interestGaps()).isEmpty();
    }

    @Test
    @DisplayName("InterestGap 갭 계산 시나리오 테스트 - 저장만 있고 읽음 없음")
    void interestGapWithOnlySavedNoRead() {
        // given
        WeeklyReportResponse.InterestGap gap = new WeeklyReportResponse.InterestGap(
                5L,
                "Business",
                10,
                0
        );

        // then
        assertThat(gap.savedCount() - gap.readCount()).isEqualTo(10);
        assertThat(Math.abs(gap.savedCount() - gap.readCount())).isEqualTo(10);
    }

    @Test
    @DisplayName("InterestGap 갭 계산 시나리오 테스트 - 읽음이 저장보다 많음")
    void interestGapWithReadMoreThanSaved() {
        // given
        WeeklyReportResponse.InterestGap gap = new WeeklyReportResponse.InterestGap(
                6L,
                "Health",
                3,
                7
        );

        // then
        assertThat(gap.readCount() - gap.savedCount()).isEqualTo(4);
        assertThat(Math.abs(gap.savedCount() - gap.readCount())).isEqualTo(4);
    }

    @Test
    @DisplayName("null이 아닌 빈 리스트로 WeeklyReportResponse 생성 테스트")
    void createWeeklyReportResponseWithEmptyList() {
        // given & when
        WeeklyReportResponse response = new WeeklyReportResponse(
                "4월 첫째주",
                "관심사를 추가해보세요!",
                5,
                2,
                3,
                2,
                4,
                1,
                new ArrayList<>()
        );

        // then
        assertThat(response.interestGaps()).isNotNull();
        assertThat(response.interestGaps()).isEmpty();
    }
}