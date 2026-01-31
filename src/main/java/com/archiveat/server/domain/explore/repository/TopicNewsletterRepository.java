package com.archiveat.server.domain.explore.repository;

import com.archiveat.server.domain.explore.entity.TopicNewsletter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TopicNewsletterRepository extends JpaRepository<TopicNewsletter, Long> {
    List<TopicNewsletter> findByNewsletterIdIn(List<Long> newsletterIds);
}
