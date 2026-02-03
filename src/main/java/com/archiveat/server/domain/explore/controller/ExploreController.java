package com.archiveat.server.domain.explore.controller;

import com.archiveat.server.domain.explore.dto.request.ClassificationRequest;
import com.archiveat.server.domain.explore.dto.response.*;
import com.archiveat.server.domain.explore.service.ExploreService;
import com.archiveat.server.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/explore")
public class ExploreController {

    private final ExploreService exploreService;

    /**
     * 사용자의 뉴스레터 보관함 상태와 카테고리별 통계를 조회합니다.
     */
    @GetMapping
    public ApiResponse<ExploreResponse> getExploreData(
            @AuthenticationPrincipal Long userId
    ) {
        ExploreResponse response = exploreService.getExploreData(userId);

        return ApiResponse.ok(response);
    }

    /**
     * 특정 토픽에 속한 뉴스레터 목록을 페이징하여 조회합니다.
     * @param topicId 조회할 토픽의 식별자
     * @param pageable 페이징 파라미터 (page, size)
     */
    @GetMapping("/topic/{topicId}/user-newsletters")
    public ApiResponse<TopicNewslettersResponse> getTopicNewsletters(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long topicId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        TopicNewslettersResponse response = exploreService.getTopicNewsletters(userId, topicId, pageable);

        return ApiResponse.ok(response);
    }

    /**
     * [방금 담은 지식(INBOX) 조회]
     * 사용자의 미확정 뉴스레터 목록을 날짜별로 그룹화하여 조회합니다.
     */
    @Operation(summary = "인박스(뉴스레터) 목록 조회", description = "사용자가 저장했고 아직 확인하지 않은 뉴스레터들을 날짜별로 묶어서 반환합니다.")
    @GetMapping("/inbox")
    public ApiResponse<InboxResponse> getInbox(
            @AuthenticationPrincipal Long userId
    ) {
        // 기존 User 객체 대신 userId를 전달하도록 변경하여 컨벤션을 통일합니다.
        InboxResponse response = exploreService.getInbox(userId);
        return ApiResponse.ok(response);
    }

    /**
     * [인박스 분류 수정 및 확정]
     * 사용자가 인박스 아이템의 카테고리와 토픽을 확정하고 메모를 저장합니다.
     */
    @Operation(summary = "인박스 분류 수정 및 확정", description = "인박스 아이템의 카테고리와 토픽을 확정하고 메모를 저장합니다.")
    @PatchMapping("/inbox/{userNewsletterId}/classification")
    public ApiResponse<ClassificationResponse> updateInboxClassification(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long userNewsletterId,
            @RequestBody ClassificationRequest request
    ) {
        ClassificationResponse response = exploreService.updateInboxClassification(userId, userNewsletterId, request);

        return ApiResponse.ok(response);
    }

    /**
     * [인박스 수정 화면 정보 조회]
     * 인박스 수정 화면 진입 시 필요한 현재 설정 값과 전체 카테고리/토픽 리스트를 반환합니다.
     */
    @Operation(summary = "인박스 수정 화면 정보 조회", description = "수정 화면 진입 시 필요한 현재 설정 값과 전체 카테고리/토픽 리스트를 반환합니다.")
    @GetMapping("/inbox/{userNewsletterId}")
    public ApiResponse<InboxEditResponse> getInboxEditData(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long userNewsletterId
    ) {
        InboxEditResponse response = exploreService.getInboxEditData(userId, userNewsletterId);

        return ApiResponse.ok(response);
    }

    /**
     * [인박스 일괄 읽음 처리]
     * 인박스 진입 시 모든 미확정 뉴스레터를 일괄적으로 확정 상태(isConfirmed=true)로 변경합니다.
     */
    @Operation(summary = "인박스 일괄 읽음 처리", description = "인박스 진입 시 모든 미확정 뉴스레터를 일괄적으로 확정 상태(isConfirmed=true)로 변경합니다.")
    @PatchMapping("/inbox/confirmation")
    public ApiResponse<Void> confirmAllInbox(
            @AuthenticationPrincipal Long userId
    ) {
        exploreService.confirmAllInbox(userId);

        return ApiResponse.ok(null);
    }

}
