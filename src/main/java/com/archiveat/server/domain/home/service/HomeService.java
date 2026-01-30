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

    private final UserRepository userRepository;
    private final UserNewsletterRepository userNewsletterRepository;
    private final CollectionRepository collectionRepository;
    private final CollectionNewsletterRepository collectionNewsletterRepository; // 추가된 의존성

    @Transactional(readOnly = true)
    public HomeResponse getHomeData(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found. id=" + userId));

        String firstGreeting = getDynamicGreeting();
        String secondGreeting = "오늘도 한 걸음 성장해볼까요?";
        List<HomeResponse.TabResponse> tabs = getTabResponses(); // 이제 메서드가 존재합니다.

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

        List<HomeResponse.ContentCollectionCardResponse> contentCollectionCards = collectionRepository.findAllByUserId(userId).stream()
                .map(col -> {
                    // 인사이트: 컬렉션은 여러 뉴스레터를 포함하므로, 매핑 엔티티를 통해 썸네일 URL들을 수집합니다.
                    List<String> thumbnailUrls = collectionNewsletterRepository.findAllByCollectionId(col.getId()).stream()
                            .map(cn -> cn.getNewsletter().getThumbnailUrl())
                            .limit(4) // 기획 디자인에 따라 최대 4개로 제한합니다.
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

    /**
     * 인사이트: Enum에 정의된 탭 정보를 기반으로 UI에 필요한 탭 리스트를 생성합니다.
     * 이렇게 분리하면 탭이 추가되거나 문구가 바뀌어도 비즈니스 로직을 수정할 필요가 없습니다.
     */
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
