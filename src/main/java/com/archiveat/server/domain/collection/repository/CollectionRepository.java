package com.archiveat.server.domain.collection.repository;

import com.archiveat.server.domain.collection.entity.Collection;
import org.springframework.data.jpa.repository.JpaRepository;

import com.archiveat.server.global.common.constant.DepthType;
import com.archiveat.server.global.common.constant.PerspectiveType;

import java.util.List;
import java.util.Optional;

public interface CollectionRepository extends JpaRepository<Collection, Long> {
    // 유저별 컬렉션 목록 조회
    List<Collection> findAllByUserId(Long userId);

    // 영역별 컬렉션 조회
    Optional<Collection> findByUserIdAndDepthTypeAndPerspectiveType(Long userId, DepthType depthType,
            PerspectiveType perspectiveType);
}
