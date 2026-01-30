package com.archiveat.server.domain.newsletter.repository;

import com.archiveat.server.domain.newsletter.entity.UserNewsletter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserNewsletterRepository extends JpaRepository<UserNewsletter, Long> {
    List<UserNewsletter> findByUserIdAndNewsletterIdIn(Long userId, List<Long> newsletterIds);

    List<UserNewsletter> findAllByUserId(Long userId);
}
