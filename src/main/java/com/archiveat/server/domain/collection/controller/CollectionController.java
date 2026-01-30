package com.archiveat.server.domain.collection.controller;

import com.archiveat.server.domain.collection.dto.response.CollectionDetailResponse;
import com.archiveat.server.domain.collection.service.CollectionService;
import com.archiveat.server.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/collections")
public class CollectionController {
    private final CollectionService collectionService;

    @GetMapping("/{collectionId}")
    public ApiResponse<CollectionDetailResponse> getCollectionDetail(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long collectionId) {
        CollectionDetailResponse response = collectionService.getCollectionDetail(userId, collectionId);
        return ApiResponse.ok(response);
    }
}
