package com.archiveat.server.domain.user.model;

import com.archiveat.server.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 30)
    private String nickname;

    @Column(nullable = false)
    private Boolean isOnboarded = false;

    @Enumerated(EnumType.STRING)
    @Column
    private EmploymentType employmentType;

    @Column
    private Short preferredTime;

    @Enumerated(EnumType.STRING)
    @Column
    private DepthType pref_morning;

    @Enumerated(EnumType.STRING)
    @Column
    private DepthType pref_lunch;

    @Enumerated(EnumType.STRING)
    @Column
    private DepthType pref_evening;

    @Enumerated(EnumType.STRING)
    @Column
    private DepthType pref_bedtime;

    @Column
    private LocalDateTime lastLoginAt;


    public User (String email, String password, String nickname) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;

    }
}
