package com.archiveat.server.domain.newsletter.controller;

import com.archiveat.server.domain.newsletter.dto.request.GenerateNewsletterRequest;
import com.archiveat.server.domain.newsletter.dto.response.DeleteNewsletterResponse;
import com.archiveat.server.domain.newsletter.dto.response.GenerateNewsletterResponse;
import com.archiveat.server.domain.newsletter.dto.response.SimpleViewNewsletterResponse;
import com.archiveat.server.domain.newsletter.dto.response.ViewNewsletterResponse;
import com.archiveat.server.domain.newsletter.service.NewsletterService;
import com.archiveat.server.global.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/newsletters")
public class NewsletterController {
        private final NewsletterService newsletterService;

        @DeleteMapping("/{userNewsletterId}")
        public ApiResponse<DeleteNewsletterResponse> deleteNewsletter(
                        @PathVariable Long userNewsletterId,
                        @AuthenticationPrincipal Long userId) {
                DeleteNewsletterResponse deleteNewsletterResponse = newsletterService.deleteUserNewsletter(
                                userId,
                                userNewsletterId);
                return ApiResponse.ok(deleteNewsletterResponse); // 204
        }

        /**
         * AI 요약 상세 조회
         * 
         * @param autoMarkRead true(기본값): 컴렉션/탐색 탭에서 자동 읽음 처리, false: 홈 탭에서 버튼 클릭 시만 처리
         */
        @GetMapping("/{userNewsletterId}")
        public ApiResponse<ViewNewsletterResponse> viewNewsletter(
                        @PathVariable Long userNewsletterId,
                        @AuthenticationPrincipal Long userId,
                        @RequestParam(defaultValue = "true") boolean autoMarkRead) {
                ViewNewsletterResponse viewNewsletterResponse = newsletterService.viewUserNewsletter(
                                userId,
                                userNewsletterId,
                                autoMarkRead);
                return ApiResponse.ok(viewNewsletterResponse);
        }

        /**
         * Simple 상세 조회
         * 
         * @param autoMarkRead true(기본값): 컴렉션/탐색 탭에서 자동 읽음 처리, false: 홈 탭에서 버튼 클릭 시만 처리
         */
        @GetMapping("/{userNewsletterId}/simple")
        public ApiResponse<SimpleViewNewsletterResponse> simpleViewNewsletter(
                        @PathVariable Long userNewsletterId,
                        @AuthenticationPrincipal Long userId,
                        @RequestParam(defaultValue = "true") boolean autoMarkRead) {
                SimpleViewNewsletterResponse simpleViewNewsletterResponse = newsletterService.simpleViewUserNewsletter(
                                userId,
                                userNewsletterId,
                                autoMarkRead);
                return ApiResponse.ok(simpleViewNewsletterResponse);
        }

        @PostMapping("")
        public ApiResponse<GenerateNewsletterResponse> generateNewsletter(
                        @AuthenticationPrincipal Long userId,
                        @Valid @RequestBody GenerateNewsletterRequest generateNewsletterRequest) {
                GenerateNewsletterResponse generateNewsletterResponse = newsletterService.generateNewsletter(
                                userId,
                                generateNewsletterRequest.getContentUrl(),
                                generateNewsletterRequest.getMemo());
                return ApiResponse.ok(generateNewsletterResponse);
        }

        @PatchMapping("/{userNewsletterId}")
        public ApiResponse<Void> updateIsRead(
                        @PathVariable Long userNewsletterId,
                        @AuthenticationPrincipal Long userId) {
                newsletterService.updateIsRead(userId, userNewsletterId);
                return ApiResponse.ok();
        }
}
