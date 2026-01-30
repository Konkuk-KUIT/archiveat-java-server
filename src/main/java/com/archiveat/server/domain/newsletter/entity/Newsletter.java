package com.archiveat.server.domain.newsletter.entity;

import com.archiveat.server.global.common.BaseEntity;
import com.archiveat.server.global.common.constant.LlmStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "newsletters")
public class Newsletter extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "domain_id")
    private Domain domain;

    private String title;

    @Column(length = 2000)
    private String thumbnailUrl;

    @Column(length = 2000, nullable = false, unique = true)
    private String contentUrl;

    // 요약 정보들
    private String smallCardSummary;

    @Column(length = 1000)
    private String mediumCardSummary;

    @Column(columnDefinition = "TEXT")
    private String newsletterSummary; // 본문 요약 (JSON 형태라면 Converter 필요)

    private Integer consumptionTimeMin; // 소비 시간(분)

    @Enumerated(EnumType.STRING)
    private LlmStatus llmStatus; // PENDING, RUNNING, DONE, FAILED

    public Newsletter(Domain domain, String contentUrl){
        this.domain = domain;
        this.title = null;
        this.thumbnailUrl = null;
        this.contentUrl = contentUrl;
        this.smallCardSummary = null;
        this.mediumCardSummary = null;
        this.newsletterSummary = null;
        this.consumptionTimeMin = null;
        this.llmStatus = LlmStatus.PENDING;
    }

    public static Newsletter createPending(Domain domain, String contentUrl){
        return new Newsletter(domain, contentUrl);
    }

    public void UpdateLlmStatus(LlmStatus llmStatus){
        this.llmStatus = llmStatus;
    }
}