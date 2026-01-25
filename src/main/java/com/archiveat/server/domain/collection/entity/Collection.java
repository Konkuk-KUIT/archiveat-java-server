package com.archiveat.server.domain.collection.entity;

import com.archiveat.server.domain.explore.entity.Topic;
import com.archiveat.server.domain.user.entity.User;
import com.archiveat.server.global.common.BaseEntity;
import com.archiveat.server.global.common.constant.DepthType;
import com.archiveat.server.global.common.constant.PerspectiveType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "collections")
public class Collection extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    private String title;
    private String smallCardSummary;
    private String mediumCardSummary;

    @Enumerated(EnumType.STRING)
    private PerspectiveType perspectiveType;

    @Enumerated(EnumType.STRING)
    private DepthType depthType;
}