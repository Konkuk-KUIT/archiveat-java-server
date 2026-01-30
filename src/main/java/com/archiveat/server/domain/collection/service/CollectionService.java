package com.archiveat.server.domain.collection.service;

import com.archiveat.server.domain.collection.dto.response.CollectionDetailResponse;
import com.archiveat.server.domain.collection.dto.response.CollectionInfoDto;
import com.archiveat.server.domain.collection.dto.response.NewsletterDto;
import com.archiveat.server.domain.collection.entity.Collection;
import com.archiveat.server.domain.collection.entity.CollectionNewsletter;
import com.archiveat.server.domain.collection.repository.CollectionNewsletterRepository;
import com.archiveat.server.domain.collection.repository.CollectionRepository;
import com.archiveat.server.domain.newsletter.entity.Newsletter;
import com.archiveat.server.domain.newsletter.entity.UserNewsletter;
import com.archiveat.server.domain.newsletter.repository.UserNewsletterRepository;
import com.archiveat.server.global.exception.CustomException;
import com.archiveat.server.global.common.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CollectionService {
    private final CollectionRepository collectionRepository;
    private final CollectionNewsletterRepository collectionNewsletterRepository;
    private final UserNewsletterRepository userNewsletterRepository;

    public CollectionDetailResponse getCollectionDetail(Long userId, Long collectionId) {
        // 1. 컬렉션 조회
        Collection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new CustomException(ErrorCode.COLLECTION_NOT_FOUND));

        // 2. 권한 확인 - 본인의 컬렉션인지 체크
        if (!collection.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        // 3. 컬렉션에 속한 뉴스레터들 조회
        List<CollectionNewsletter> collectionNewsletters = collectionNewsletterRepository
                .findByCollectionId(collectionId);

        // 4. Newsletter ID 목록 추출
        List<Long> newsletterIds = collectionNewsletters.stream()
                .map(cn -> cn.getNewsletter().getId())
                .collect(Collectors.toList());

        // 5. UserNewsletter 정보 조회 (memo와 isRead 포함)
        List<UserNewsletter> userNewsletters = userNewsletterRepository.findByUserIdAndNewsletterIdIn(userId,
                newsletterIds);

        // 6. Newsletter ID를 키로 하는 Map 생성 (빠른 조회를 위해)
        Map<Long, UserNewsletter> userNewsletterMap = userNewsletters.stream()
                .collect(Collectors.toMap(
                        un -> un.getNewsletter().getId(),
                        un -> un));

        // 7. 읽은 개수 계산
        long readCount = userNewsletters.stream()
                .filter(UserNewsletter::isRead)
                .count();

        // 8. CollectionInfo 생성
        CollectionInfoDto collectionInfo = new CollectionInfoDto(
                collection.getId(),
                collection.getUser().getNickname(),
                collection.getTopic().getName(),
                collectionNewsletters.size(),
                (int) readCount);

        // 9. Newsletter DTO 리스트 생성
        List<NewsletterDto> newsletters = collectionNewsletters.stream()
                .map(cn -> {
                    Newsletter newsletter = cn.getNewsletter();
                    UserNewsletter userNewsletter = userNewsletterMap.get(newsletter.getId());

                    return new NewsletterDto(
                            newsletter.getId(),
                            newsletter.getDomain().getName(),
                            newsletter.getTitle(),
                            newsletter.getThumbnailUrl(),
                            newsletter.getConsumptionTimeMin(),
                            userNewsletter != null ? userNewsletter.getMemo() : "",
                            userNewsletter != null && userNewsletter.isRead());
                })
                .collect(Collectors.toList());

        return new CollectionDetailResponse(collectionInfo, newsletters);
    }
}
