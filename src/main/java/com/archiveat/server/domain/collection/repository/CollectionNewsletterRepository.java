package com.archiveat.server.domain.collection.repository;

import com.archiveat.server.domain.collection.entity.CollectionNewsletter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CollectionNewsletterRepository extends JpaRepository<CollectionNewsletter, Long> {
    // 특정 컬렉션에 포함된 모든 뉴스레터 연결 정보를 조회합니다.
    List<CollectionNewsletter> findAllByCollectionId(Long collectionId);
}
