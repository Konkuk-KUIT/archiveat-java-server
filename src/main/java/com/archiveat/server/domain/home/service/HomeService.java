package com.archiveat.server.domain.home.service;

import com.archiveat.server.domain.collection.repository.CollectionNewsletterRepository;
import com.archiveat.server.domain.collection.repository.CollectionRepository;
import com.archiveat.server.domain.home.dto.response.HomeResponse;
import com.archiveat.server.domain.newsletter.repository.UserNewsletterRepository;
import com.archiveat.server.domain.user.entity.User;
import com.archiveat.server.domain.user.repository.UserRepository;
import com.archiveat.server.global.common.constant.DepthType;
import com.archiveat.server.global.common.constant.HomeTabType;
import com.archiveat.server.global.common.constant.PerspectiveType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HomeService {

    private final UserNewsletterRepository userNewsletterRepository;
    private final CollectionRepository collectionRepository;
    private final CollectionNewsletterRepository collectionNewsletterRepository; // 추가된 의존성

    @Transactional(readOnly = true)
    public HomeResponse getHomeData(Long userId) {
        String firstGreeting = getDynamicGreeting();
        String secondGreeting = "오늘도 한 걸음 성장해볼까요?";
        List<HomeResponse.TabResponse> tabs = getTabResponses();

        // 1. 뉴스레터 카드 데이터 조회
        // 전달받은 userId를 리포지토리에 직접 사용하여 DB 쿼리 효율을 높입니다.
        List<HomeResponse.ContentCardResponse> contentCards = userNewsletterRepository.findAllByUserId(userId).stream()
                .map(un -> new HomeResponse.ContentCardResponse(
                        un.getNewsletter().getId(),
                        determineTabLabel(un.getPerspectiveType(), un.getDepthType()),
                        "AI 요약",
                        un.getNewsletter().getTitle(),
                        un.getNewsletter().getSmallCardSummary(),
                        un.getNewsletter().getMediumCardSummary(),
                        un.getNewsletter().getThumbnailUrl()
                ))
                .collect(Collectors.toList());

        // 2. 컬렉션 카드 데이터 조회
        List<HomeResponse.ContentCollectionCardResponse> contentCollectionCards = collectionRepository.findAllByUserId(userId).stream()
                .map(col -> {
                    List<String> thumbnailUrls = collectionNewsletterRepository.findAllByCollectionId(col.getId()).stream()
                            .map(cn -> cn.getNewsletter().getThumbnailUrl())
                            .limit(4)
                            .collect(Collectors.toList());

                    return new HomeResponse.ContentCollectionCardResponse(
                            col.getId(),
                            determineTabLabel(col.getPerspectiveType(), col.getDepthType()),
                            "컬렉션",
                            col.getTitle(),
                            col.getSmallCardSummary(),
                            col.getMediumCardSummary(),
                            thumbnailUrls
                    );
                })
                .collect(Collectors.toList());

        return new HomeResponse(firstGreeting, secondGreeting, tabs, contentCards, contentCollectionCards);
    }


    private List<HomeResponse.TabResponse> getTabResponses() {
        return Arrays.stream(HomeTabType.values())
                .map(tab -> new HomeResponse.TabResponse(
                        tab.name(),
                        tab.getLabel(),
                        tab.getSubMessage()
                ))
                .collect(Collectors.toList());
    }

    /**
     * PerspectiveType과 DepthType의 조합에 따라 적절한 탭 라벨을 반환합니다.
     */
    private String determineTabLabel(PerspectiveType pType, DepthType dType) {
        if (pType == PerspectiveType.NOW) {
            return dType == DepthType.LIGHT ? HomeTabType.INSPIRATION.getLabel() : HomeTabType.DEEP_DIVE.getLabel();
        } else {
            return dType == DepthType.LIGHT ? HomeTabType.GROWTH.getLabel() : HomeTabType.VIEW_EXPANSION.getLabel();
        }
    }

    /**
     * 현재 시간을 기준으로 아침/밤 인사말을 결정합니다.
     */
    private String getDynamicGreeting() {
        LocalTime now = LocalTime.now();
        // 오전 5시 ~ 오후 6시 이전까지는 "좋은 아침", 그 외 시간은 "좋은 밤"
        if (now.isAfter(LocalTime.of(5, 0)) && now.isBefore(LocalTime.of(18, 0))) {
            return "좋은 아침이에요!";
        }
        return "좋은 밤이에요!";
    }

}
