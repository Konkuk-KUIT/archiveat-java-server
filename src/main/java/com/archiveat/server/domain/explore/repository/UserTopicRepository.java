package com.archiveat.server.domain.explore.repository;

import com.archiveat.server.domain.explore.entity.UserTopic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserTopicRepository extends JpaRepository<UserTopic, Long> {
    // 특정 유저의 기존 관심사를 한 번에 삭제하기 위한 쿼리 메서드입니다.
    void deleteAllByUserId(Long userId);
}
