package com.archiveat.server.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;
// 해당 파일은 현재는 사용하지 않음
// 확장 가능성을 대비한 테이블
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "oauth")
public class Oauth {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private Long providerUserId;
    private String provider;
    private Long version;
}