package com.archiveat.server.domain.explore.repository;

import com.archiveat.server.domain.explore.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TopicRepository extends JpaRepository<Topic, Long> {
}
