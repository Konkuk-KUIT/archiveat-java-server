package com.archiveat.server.domain.collection.repository;

import com.archiveat.server.domain.collection.entity.Collection;
import org.springframework.data.jpa.repository.JpaRepository;

import com.archiveat.server.global.common.constant.DepthType;
import com.archiveat.server.global.common.constant.PerspectiveType;

import java.util.List;

public interface CollectionRepository extends JpaRepository<Collection, Long> {
    // 유저별 컬렉션 목록 조회
    List<Collection> findAllByUserId(Long userId);

    // 영역별 컬렉션 조회
    /**
     * 특정 유저, DepthType, PerspectiveType에 해당하는 컬렉션 조회
     * Unique 제약 조건이 없던 시기의 중복 데이터 처리를 위해 List로 반환
     */
    List<Collection> findByUserIdAndDepthTypeAndPerspectiveType(Long userId, DepthType depthType,
            PerspectiveType perspectiveType);
}
