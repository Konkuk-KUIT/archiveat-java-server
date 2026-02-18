package com.archiveat.server.domain.collection.entity;

import com.archiveat.server.domain.explore.entity.Topic;
import com.archiveat.server.domain.user.entity.User;
import com.archiveat.server.global.common.BaseEntity;
import com.archiveat.server.global.common.constant.DepthType;
import com.archiveat.server.global.common.constant.PerspectiveType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "collections", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "user_id", "depth_type", "perspective_type" })
})
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
    @Column(name = "perspective_type") // Explicit column name for clarity
    private PerspectiveType perspectiveType;

    @Enumerated(EnumType.STRING)
    @Column(name = "depth_type") // Explicit column name for clarity
    private DepthType depthType;

    @OneToMany(mappedBy = "collection", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CollectionNewsletter> collectionNewsletters = new ArrayList<>();

    @Builder
    public Collection(Long id, User user, Topic topic, String title, String smallCardSummary, String mediumCardSummary,
                      PerspectiveType perspectiveType, DepthType depthType) {
        this.id = id;
        this.user = user;
        this.topic = topic;
        this.title = title;
        this.smallCardSummary = smallCardSummary;
        this.mediumCardSummary = mediumCardSummary;
        this.perspectiveType = perspectiveType;
        this.depthType = depthType;
    }
}