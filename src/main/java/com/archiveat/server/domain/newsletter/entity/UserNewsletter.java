package com.archiveat.server.domain.newsletter.entity;

import com.archiveat.server.domain.user.entity.User;
import com.archiveat.server.global.common.BaseEntity;
import com.archiveat.server.global.common.constant.DepthType;
import com.archiveat.server.global.common.constant.PerspectiveType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_newsletters")
public class UserNewsletter extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // userNewsletterId

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "newsletter_id")
    private Newsletter newsletter;

    @Column(columnDefinition = "TEXT")
    private String memo;

    @Enumerated(EnumType.STRING)
    private PerspectiveType perspectiveType;

    @Enumerated(EnumType.STRING)
    private DepthType depthType;

    private boolean isRead;
    private boolean isConfirmed;
    private LocalDateTime lastViewedAt;
    private LocalDateTime confirmedAt;

    public UserNewsletter(User user, Newsletter newsletter, String memo) {
        this.user = user;
        this.newsletter = newsletter;
        this.memo = memo;
        this.perspectiveType = null;
        this.depthType = null;
        this.isRead = false;
        this.isConfirmed = false;
    }

    public static UserNewsletter create(User user, Newsletter newsletter, String memo) {
        return new UserNewsletter(user, newsletter, memo);
    }

    /**
     * 분류 정보 및 메모 업데이트
     */
    public void updateClassification(String memo) {
        this.memo = memo;
        this.isConfirmed = true;
        this.confirmedAt = LocalDateTime.now();
    }

    public void updateIsRead() {
        this.isRead = true;
        updateIsConfirmed();
        updateLastViewedAt();
    }

    public void updateIsConfirmed() {
        this.isConfirmed = true;
    }

    public void updateLastViewedAt() {
        this.lastViewedAt = LocalDateTime.now();
    }

    /**
     * Label 구성 요소 업데이트 (LLM 처리 완료 후 호출)
     */
    public void updateLabelComponents(PerspectiveType perspectiveType, DepthType depthType) {
        this.perspectiveType = perspectiveType;
        this.depthType = depthType;
    }
}