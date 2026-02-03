package com.archiveat.server.domain.explore.repository;

import com.archiveat.server.domain.explore.entity.UserTopic;
import com.archiveat.server.global.common.constant.PerspectiveType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface UserTopicRepository extends JpaRepository<UserTopic, Long> {
    // 특정 유저의 기존 관심사를 한 번에 삭제하기 위한 쿼리 메서드입니다.
    void deleteAllByUserId(Long userId);

    /**
     * 특정 유저의 NOW 관심사 카테고리 이름 목록 조회
     * Label 계산에 사용 (NOW인 카테고리들의 이름을 반환)
     */
    @Query("SELECT DISTINCT t.category.name FROM UserTopic ut " +
            "JOIN ut.topic t " +
            "WHERE ut.user.id = :userId " +
            "AND ut.perspectiveType = :perspectiveType")
    List<String> findCategoryNamesByUserIdAndPerspectiveType(@Param("userId") Long userId,
            @Param("perspectiveType") PerspectiveType perspectiveType);
}
