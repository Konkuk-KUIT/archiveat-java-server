package com.archiveat.server.domain.user.entity;

import com.archiveat.server.domain.user.dto.request.OnboardingInfoRequest;
import com.archiveat.server.global.common.BaseEntity;
import com.archiveat.server.global.common.constant.DepthType;
import com.archiveat.server.global.common.constant.EmploymentType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users")
public class User extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String password; // 소셜 로그인이면 null 가능
    private String nickname;

    @Enumerated(EnumType.STRING)
    private EmploymentType employmentType;

    @Enumerated(EnumType.STRING)
    private DepthType prefMorning;
    @Enumerated(EnumType.STRING)
    private DepthType prefLunch;
    @Enumerated(EnumType.STRING)
    private DepthType prefEvening;
    @Enumerated(EnumType.STRING)
    private DepthType prefBedtime;

    private Integer commuteDurationMin;
    private LocalDateTime lastLoginAt;

    @Column(length = 1000)
    private String refreshToken;

    @Builder
    public User(String email, String nickname, EmploymentType employmentType) {
        this.email = email;
        this.nickname = nickname;
        this.employmentType = employmentType;
    }

    public User(String email, String encoded, String nickname) {
        this.email = email;
        this.password = encoded;
        this.nickname = nickname;
    }

    public void updateLastLoginAt() {
        this.lastLoginAt = LocalDateTime.now();
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updateOnboardingInfo(EmploymentType employmentType,
                                     OnboardingInfoRequest.AvailabilityRequest availability) {
        this.employmentType = employmentType;
        this.prefMorning = availability.pref_morning();
        this.prefLunch = availability.pref_lunch();
        this.prefEvening = availability.pref_evening();
        this.prefBedtime = availability.pref_bedtime();
    }

    // refresh 토큰 저장/삭제/검증 편의 메서드
    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public void clearRefreshToken() {
        this.refreshToken = null;
    }

    public boolean matchesRefreshToken(String refreshToken) {
        return this.refreshToken != null && this.refreshToken.equals(refreshToken);
    }
}