package com.archiveat.server.domain.collection.dto.response;

import java.util.List;

public record CollectionDetailResponse(
        CollectionInfoDto collectionInfo,
        List<NewsletterDto> newsletters) {
}
