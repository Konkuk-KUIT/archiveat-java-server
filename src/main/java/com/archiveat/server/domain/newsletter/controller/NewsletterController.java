package com.archiveat.server.domain.newsletter.controller;

import com.archiveat.server.domain.newsletter.dto.request.GenerateNewsletterRequest;
import com.archiveat.server.domain.newsletter.dto.response.DeleteNewsletterResponse;
import com.archiveat.server.domain.newsletter.dto.response.GenerateNewsletterResponse;
import com.archiveat.server.domain.newsletter.dto.response.SimpleViewNewsletterResponse;
import com.archiveat.server.domain.newsletter.dto.response.ViewNewsletterResponse;
import com.archiveat.server.domain.newsletter.service.NewsletterService;
import com.archiveat.server.global.common.response.ApiResponse;
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
            @AuthenticationPrincipal Long userId
    ) {
        DeleteNewsletterResponse deleteNewsletterResponse = newsletterService.deleteUserNewsletter(
                userId,
                userNewsletterId
        );
        return ApiResponse.ok(deleteNewsletterResponse); // 204
    }

    @GetMapping("/{userNewsletterId}")
    public ApiResponse<ViewNewsletterResponse> viewNewsletter(
            @PathVariable Long userNewsletterId,
            @AuthenticationPrincipal Long userId
    ) {
        ViewNewsletterResponse viewNewsletterResponse = newsletterService.viewUserNewsletter(
                userId,
                userNewsletterId
        );
        return ApiResponse.ok(viewNewsletterResponse); // 204
    }

    @GetMapping("/{userNewsletterId}/simple")
    public ApiResponse<SimpleViewNewsletterResponse> simpleViewNewsletter(
            @PathVariable Long userNewsletterId,
            @AuthenticationPrincipal Long userId
    ) {
        SimpleViewNewsletterResponse simpleViewNewsletterResponse = newsletterService.simpleViewUserNewsletter(
                userId,
                userNewsletterId
        );
        return ApiResponse.ok(simpleViewNewsletterResponse); // 204
    }

    @PostMapping("")
    public ApiResponse<GenerateNewsletterResponse> generateNewsletter(
            @AuthenticationPrincipal Long userId,
            @RequestBody GenerateNewsletterRequest generateNewsletterRequest
    ){
        GenerateNewsletterResponse generateNewsletterResponse = newsletterService.generateNewsletter(
                userId,
                generateNewsletterRequest.getContentUrl(),
                generateNewsletterRequest.getMemo()
        );
        return ApiResponse.ok(generateNewsletterResponse);
    }

    @PatchMapping("/{userNewsletterId}")
    public ApiResponse<Void> updateIsRead(
            @PathVariable Long userNewsletterId,
            @AuthenticationPrincipal Long userId
    ){
        newsletterService.updateIsRead(userId, userNewsletterId);
        return ApiResponse.ok();
    }
}
