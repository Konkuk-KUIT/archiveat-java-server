package com.archiveat.server.domain.explore.controller;

import com.archiveat.server.domain.explore.dto.request.ClassificationRequest;
import com.archiveat.server.domain.explore.dto.response.*;
import com.archiveat.server.domain.explore.service.ExploreService;
import com.archiveat.server.global.common.constant.LlmStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ExploreController.class)
public class ExploreControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean // [Insight] @MockBean 대신 사용되는 최신 어노테이션입니다.
    private ExploreService exploreService;

    @Test
    @WithMockUser // [Reason] 인증된 사용자 정보를 컨텍스트에 담아 보안 필터를 통과시킵니다.
    @DisplayName("탐색 메인 데이터 조회 API 검증")
    void getExploreData_ApiSuccess() throws Exception {
        ExploreResponse response = new ExploreResponse(5, LlmStatus.DONE, List.of());
        when(exploreService.getExploreData(anyLong())).thenReturn(response);

        mockMvc.perform(get("/explore"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.data.inboxCount").value(5))
                .andExpect(jsonPath("$.data.llmStatus").value("DONE"));
    }

    @Test
    @WithMockUser
    @DisplayName("인박스(INBOX) 목록 조회 API: 날짜별 그룹 구조 검증")
    void getInbox_ApiSuccess() throws Exception {
        InboxResponse.InboxItemDto item = InboxResponse.InboxItemDto.builder()
                .title("테스트 뉴스레터")
                .llmStatus(LlmStatus.DONE)
                .build();

        InboxResponse.InboxDateGroupDto group = InboxResponse.InboxDateGroupDto.builder()
                .date("2026-02-09")
                .items(List.of(item))
                .build();

        InboxResponse response = InboxResponse.builder().inbox(List.of(group)).build();
        when(exploreService.getInbox(anyLong())).thenReturn(response);

        mockMvc.perform(get("/explore/inbox"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.inbox[0].date").value("2026-02-09"))
                .andExpect(jsonPath("$.data.inbox[0].items[0].title").value("테스트 뉴스레터"));
    }

    @Test
    @WithMockUser
    @DisplayName("인박스 분류 수정 및 확정 API: 요청/응답 동기화 검증")
    void updateInboxClassification_ApiSuccess() throws Exception {
        ClassificationRequest request = new ClassificationRequest(1L, 10L, "수정 메모");
        ClassificationResponse response = ClassificationResponse.builder()
                .memo("수정 메모")
                .userNewsletterId(100L)
                .build();

        when(exploreService.updateInboxClassification(anyLong(), anyLong(), any(ClassificationRequest.class)))
                .thenReturn(response);

        mockMvc.perform(patch("/explore/inbox/{userNewsletterId}/classification", 100L)
                        .with(csrf()) // [Insight] 이제 정상적으로 인식됩니다.
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memo").value("수정 메모"))
                .andExpect(jsonPath("$.data.userNewsletterId").value(100L));
    }

    @Test
    @WithMockUser
    @DisplayName("인박스 일괄 읽음 처리 API 검증")
    void confirmAllInbox_ApiSuccess() throws Exception {
        mockMvc.perform(patch("/explore/inbox/confirmation")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true));
    }
}
