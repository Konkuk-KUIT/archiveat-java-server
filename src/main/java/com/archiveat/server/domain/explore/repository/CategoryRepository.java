package com.archiveat.server.domain.explore.repository;

import com.archiveat.server.domain.explore.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByName(String name);

    /**
     * Category 조회 시 Topics를 JOIN FETCH하여
     * N+1 문제를 방지
     */
    @Override
    @Query("SELECT DISTINCT c FROM Category c LEFT JOIN FETCH c.topics")
    List<Category> findAll();
}
