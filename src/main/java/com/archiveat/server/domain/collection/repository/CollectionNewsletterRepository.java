package com.archiveat.server.domain.collection.repository;

import com.archiveat.server.domain.collection.entity.CollectionNewsletter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CollectionNewsletterRepository extends JpaRepository<CollectionNewsletter, Long> {
    List<CollectionNewsletter> findByCollectionId(Long collectionId);
}
