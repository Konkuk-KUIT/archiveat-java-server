package com.archiveat.server.domain.newsletter.entity;

import com.archiveat.server.domain.newsletter.dto.response.PythonSummaryResponse;
import com.archiveat.server.global.common.BaseEntity;
import com.archiveat.server.global.common.constant.LlmStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "newsletters")
public class Newsletter extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "domain_id")
    private Domain domain;

    private String title;

    @Column(length = 2000)
    private String thumbnailUrl;

    @Column(length = 2000, nullable = false, unique = true)
    private String contentUrl;

    // 카테고리 및 토픽
    private String category;
    private String topic;

    // 요약 정보들
    private String smallCardSummary;

    @Column(length = 1000)
    private String mediumCardSummary;

    @Column(columnDefinition = "TEXT")
    private String newsletterSummary; // 본문 요약 (JSON 형태)

    private Integer consumptionTimeMin; // 소비 시간(분)

    @Enumerated(EnumType.STRING)
    private LlmStatus llmStatus; // PENDING, RUNNING, DONE, FAILED

    @Column(length = 500)
    private String errorMessage; // 에러 메시지 (FAILED 상태일 때)

    public Newsletter(Domain domain, String contentUrl) {
        this.domain = domain;
        this.title = null;
        this.thumbnailUrl = null;
        this.contentUrl = contentUrl;
        this.category = null;
        this.topic = null;
        this.smallCardSummary = null;
        this.mediumCardSummary = null;
        this.newsletterSummary = null;
        this.consumptionTimeMin = null;
        this.llmStatus = LlmStatus.PENDING;
        this.errorMessage = null;
    }

    public static Newsletter createPending(Domain domain, String contentUrl) {
        return new Newsletter(domain, contentUrl);
    }

    public void updateLlmStatus(LlmStatus llmStatus) {
        this.llmStatus = llmStatus;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        this.llmStatus = LlmStatus.FAILED;
    }

    /**
     * Python 서버 응답으로부터 Newsletter 엔티티 업데이트
     */
    public void updateFromPythonResponse(PythonSummaryResponse response) {
        PythonSummaryResponse.Analysis analysis = response.getAnalysis();

        // 카테고리 및 토픽 (필드명 변경됨: category → categoryName, topic → topicName)
        this.category = analysis.getCategoryName();
        this.topic = analysis.getTopicName();

        // 요약 정보
        this.smallCardSummary = analysis.getSmallCardSummary();
        this.mediumCardSummary = analysis.getMediumCardSummary();

        // newsletter_summary를 JSON 문자열로 저장
        try {
            ObjectMapper mapper = new ObjectMapper();
            this.newsletterSummary = mapper.writeValueAsString(analysis.getNewsletterSummary());
        } catch (JsonProcessingException e) {
            this.newsletterSummary = "[]"; // 에러 시 빈 배열
        }

        // video_info가 있으면 업데이트 (YouTube의 경우)
        if (response.getVideoInfo() != null) {
            PythonSummaryResponse.VideoInfo videoInfo = response.getVideoInfo();
            this.title = videoInfo.getTitle();
            this.thumbnailUrl = videoInfo.getThumbnailUrl();

            // duration을 분 단위로 변환 (초 -> 분)
            if (videoInfo.getDuration() != null) {
                this.consumptionTimeMin = (int) Math.ceil(videoInfo.getDuration() / 60.0);
            }
        }
        // article_info가 있으면 업데이트 (Naver News, 일반 웹의 경우)
        else if (response.getArticleInfo() != null) {
            PythonSummaryResponse.ArticleInfo articleInfo = response.getArticleInfo();
            this.title = articleInfo.getTitle();
            this.thumbnailUrl = articleInfo.getThumbnailUrl();

            // word_count를 분 단위로 변환 (분당 400자 기준)
            if (articleInfo.getWordCount() != null) {
                this.consumptionTimeMin = (int) Math.ceil(articleInfo.getWordCount() / 400.0);
            }
        }

        // 상태를 DONE으로 변경
        this.llmStatus = LlmStatus.DONE;
    }
}
