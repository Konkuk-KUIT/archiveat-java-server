package com.archiveat.server.domain.collection.entity;

import com.archiveat.server.domain.newsletter.entity.Newsletter;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "collection_newsletters")
public class CollectionNewsletter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_id")
    private Collection collection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "newsletter_id")
    private Newsletter newsletter;

    @Builder
    public CollectionNewsletter(Collection collection, Newsletter newsletter) {
        this.collection = collection;
        this.newsletter = newsletter;
    }
}