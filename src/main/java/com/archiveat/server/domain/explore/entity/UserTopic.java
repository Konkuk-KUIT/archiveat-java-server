package com.archiveat.server.domain.explore.entity;

import com.archiveat.server.domain.user.entity.User;
import com.archiveat.server.global.common.constant.PerspectiveType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA를 위한 기본 생성자
@Table(name = "user_topics")
public class UserTopic {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @Enumerated(EnumType.STRING)
    private PerspectiveType perspectiveType;

    @Builder
    public UserTopic(Long id, User user, Topic topic, PerspectiveType perspectiveType) {
        this.id = id;
        this.user = user;
        this.topic = topic;
        this.perspectiveType = perspectiveType;
    }
}