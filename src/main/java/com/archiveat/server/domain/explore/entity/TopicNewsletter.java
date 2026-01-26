package com.archiveat.server.domain.explore.entity;

import com.archiveat.server.domain.newsletter.entity.Newsletter;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
@Table(name = "topic_newsletters")
public class TopicNewsletter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "newsletter_id")
    private Newsletter newsletter;
}
