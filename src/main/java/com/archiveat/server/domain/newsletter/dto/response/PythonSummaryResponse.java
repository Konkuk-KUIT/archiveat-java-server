package com.archiveat.server.domain.newsletter.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PythonSummaryResponse {

    @JsonProperty("video_info")
    private VideoInfo videoInfo;

    @JsonProperty("article_info")
    private ArticleInfo articleInfo;

    private Analysis analysis;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VideoInfo {
        private String title;

        @JsonProperty("thumbnail_url")
        private String thumbnailUrl;

        @JsonProperty("content_url")
        private String contentUrl;

        private String channel;
        private Integer duration; // 초 단위
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ArticleInfo {
        private String title;

        @JsonProperty("thumbnail_url")
        private String thumbnailUrl;

        @JsonProperty("content_url")
        private String contentUrl;

        @JsonProperty("word_count")
        private Integer wordCount; // 글자 수
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Analysis {
        @JsonProperty("category")
        private String categoryName;

        @JsonProperty("topic")
        private String topicName;

        @JsonProperty("small_card_summary")
        private String smallCardSummary;

        @JsonProperty("medium_card_summary")
        private String mediumCardSummary;

        @JsonProperty("newsletter_summary")
        private List<NewsletterSummaryBlock> newsletterSummary;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NewsletterSummaryBlock {
        private String title;
        private String content;
    }
}
