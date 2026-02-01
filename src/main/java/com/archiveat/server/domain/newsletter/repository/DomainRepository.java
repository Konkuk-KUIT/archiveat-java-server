package com.archiveat.server.domain.newsletter.repository;

import com.archiveat.server.domain.newsletter.entity.Domain;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DomainRepository extends JpaRepository<Domain, Long> {
    Optional<Domain> findByName(String name);
}
