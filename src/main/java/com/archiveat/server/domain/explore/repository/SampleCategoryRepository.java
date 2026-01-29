package com.archiveat.server.domain.explore.repository;

import com.archiveat.server.domain.explore.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SampleCategoryRepository extends JpaRepository<Category, Long> {
    // todo: 혹시 겹칠까봐 임시로 Sample이라고 만듦. 이거 지금 온보딩 메타데이터 조회쪽에 사용중
}
