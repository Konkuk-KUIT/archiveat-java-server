package com.archiveat.server.domain.newsletter.repository;

import com.archiveat.server.domain.newsletter.entity.Newsletter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NewsletterRepository extends JpaRepository<Newsletter, Long> {
    Optional<Newsletter> findByContentUrl(String contentUrl);
}
