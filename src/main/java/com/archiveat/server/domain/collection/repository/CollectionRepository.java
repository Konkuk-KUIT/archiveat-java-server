package com.archiveat.server.domain.collection.repository;

import com.archiveat.server.domain.collection.entity.Collection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CollectionRepository extends JpaRepository<Collection, Long> {
    // 유저별 컬렉션 목록 조회
    List<Collection> findAllByUserId(Long userId);
}
