package com.archiveat.server.domain.newsletter.repository;

import com.archiveat.server.domain.newsletter.entity.UserNewsletter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserNewsletterRepository extends JpaRepository<UserNewsletter, Long> {
    int deleteByIdAndUserId(Long id, Long userId);
    Optional<UserNewsletter> findByIdAndUser_Id(Long id, Long userId);
}
