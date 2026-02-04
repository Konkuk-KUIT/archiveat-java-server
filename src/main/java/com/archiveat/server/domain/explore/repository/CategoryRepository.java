package com.archiveat.server.domain.explore.repository;

import com.archiveat.server.domain.explore.entity.Category;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByName(String name);
    /**
     * @EntityGraph를 사용하여 Category 조회 시 Topics를 FETCH JOIN
     * N+1 문제를 방지
     */
    @Override
    @EntityGraph(attributePaths = "topics")
    List<Category> findAll();
}
