package com.archiveat.server.domain.collection.dto.response;

public record CollectionInfoDto(
        Long collectionId,
        String userNickname,
        String topicName,
        Integer totalCount,
        Integer readCount) {
}
