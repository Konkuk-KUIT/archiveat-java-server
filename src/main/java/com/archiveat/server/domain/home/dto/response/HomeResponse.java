package com.archiveat.server.domain.home.dto.response;

import java.util.List;

/**
 * 홈 화면 조회 시 반환되는 통합 응답 DTO입니다.
 */
public record HomeResponse(
                String firstGreetingMessage,
                String secondGreetingMessage,
                List<TabResponse> tabs,
                List<ContentCardResponse> contentCards,
                List<ContentCollectionCardResponse> contentCollectionCards) {
        /**
         * 상단 탭 정보를 담는 DTO입니다.
         */
        public record TabResponse(
                        String type, // ALL, INSPIRATION, DEEP_DIVE 등
                        String label,
                        String subMessage) {
        }

        /**
         * 개별 뉴스레터(AI 요약) 카드를 담는 DTO입니다.
         */
        public record ContentCardResponse(
                        Long userNewsletterId,
                        String tabLabel,
                        String cardType, // "AI 요약"
                        String title,
                        String smallCardSummary,
                        String mediumCardSummary,
                        String thumbnailUrl,
                        String domainName,
                        String createdAt) {
        }

        /**
         * 뉴스레터 꾸러미(컬렉션) 카드를 담는 DTO입니다.
         */
        public record ContentCollectionCardResponse(
                        Long collectionId,
                        String tabLabel,
                        String cardType, // "컬렉션"
                        String title,
                        String smallCardSummary,
                        String mediumCardSummary,
                        List<ThumbnailInfo> thumbnails) {
        }

        /**
         * 썸네일 정보와 도메인 이름을 함께 담는 DTO입니다.
         */
        public record ThumbnailInfo(
                        String thumbnailUrl,
                        String domainName) {
        }
}
