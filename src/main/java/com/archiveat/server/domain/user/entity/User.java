package com.archiveat.server.domain.user.entity;

import com.archiveat.server.global.common.BaseEntity;
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

    private Integer commuteDurationMin;
    private LocalDateTime lastLoginAt;

    @Builder
    public User(String email, String nickname, EmploymentType employmentType) {
        this.email = email;
        this.nickname = nickname;
        this.employmentType = employmentType;
    }

    public User(String email, String encoded, String nickname) {
        super();
    }
}