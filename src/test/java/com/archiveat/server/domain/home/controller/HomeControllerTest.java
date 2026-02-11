package com.archiveat.server.domain.home.controller;

import com.archiveat.server.domain.explore.controller.ExploreController;
import com.archiveat.server.domain.home.dto.response.HomeResponse;
import com.archiveat.server.domain.home.service.HomeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HomeController.class)
public class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private HomeService homeService;

    @Test
    @WithMockUser(username = "1")
    @DisplayName("[Home] 홈 데이터 조회 API 검증")
    void getHomeData_ApiSuccess() throws Exception {
        // given
        HomeResponse.TabResponse tab = new HomeResponse.TabResponse("INSPIRATION", "영감수집", "잠깐의 틈을 채워줄 인사이트");
        HomeResponse.ContentCardResponse card = new HomeResponse.ContentCardResponse(
                10L, "영감수집", "AI 요약", "테스트 뉴스레터", "소형 요약", "중형 요약", "thumb.jpg"
        );
        HomeResponse.ContentCollectionCardResponse collection = new HomeResponse.ContentCollectionCardResponse(
                100L, "집중탐구", "컬렉션", "테스트 컬렉션", "소형 요약", "중형 요약", List.of("thumb1.jpg", "thumb2.jpg")
        );

        HomeResponse homeResponse = new HomeResponse(
                "좋은 아침이에요!",
                "오늘도 한 걸음 성장해볼까요?",
                List.of(tab),
                List.of(card),
                List.of(collection)
        );

        when(homeService.getHomeData(any())).thenReturn(homeResponse);

        // when & then
        mockMvc.perform(get("/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                // 인사말 검증
                .andExpect(jsonPath("$.data.firstGreetingMessage").value("좋은 아침이에요!"))
                // 탭 검증 (리스트의 0번째 요소의 필드들 확인)
                .andExpect(jsonPath("$.data.tabs[0].label").value("영감수집"))
                .andExpect(jsonPath("$.data.tabs[0].type").value("INSPIRATION"))
                // 뉴스레터 카드 검증
                .andExpect(jsonPath("$.data.contentCards[0].title").value("테스트 뉴스레터"))
                .andExpect(jsonPath("$.data.contentCards[0].newsletterId").value(10))
                // 컬렉션 카드 검증
                .andExpect(jsonPath("$.data.contentCollectionCards[0].title").value("테스트 컬렉션"))
                .andExpect(jsonPath("$.data.contentCollectionCards[0].thumbnailUrls").isArray())
                .andExpect(jsonPath("$.data.contentCollectionCards[0].thumbnailUrls[0]").value("thumb1.jpg"));
    }
}
