package com.archiveat.server.domain.report.entity;

import com.archiveat.server.domain.user.entity.User;
import com.archiveat.server.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "reports")
public class Report extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private Integer totalNewsletterCount;
    private Integer consumedNewsletterCount;

    private BigDecimal lightConsumedBalance;
    private BigDecimal deepConsumedBalance;
    private BigDecimal nowConsumedBalance;
    private BigDecimal futureConsumedBalance;

    @Builder
    public Report(User user, Integer totalNewsletterCount, Integer consumedNewsletterCount,
                  BigDecimal lightConsumedBalance, BigDecimal deepConsumedBalance,
                  BigDecimal nowConsumedBalance, BigDecimal futureConsumedBalance) {
        this.user = user;
        this.totalNewsletterCount = totalNewsletterCount;
        this.consumedNewsletterCount = consumedNewsletterCount;
        this.lightConsumedBalance = lightConsumedBalance;
        this.deepConsumedBalance = deepConsumedBalance;
        this.nowConsumedBalance = nowConsumedBalance;
        this.futureConsumedBalance = futureConsumedBalance;
    }

    // AI 피드백 멘트 필드 추가 권장 (text feedbackMessage)
}