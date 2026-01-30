package com.archiveat.server.domain.explore.entity;

import com.archiveat.server.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
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

    /**
     * 서비스 레이어에서 UserTopic 객체를 생성하기 위한 생성자입니다.
     * @param user 관심사를 등록하는 사용자
     * @param topic 사용자가 선택한 관심 토픽
     */
    public UserTopic(User user, Topic topic) {
        this.user = user;
        this.topic = topic;
    }
}