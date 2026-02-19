package com.archiveat.server.domain.home.service;

import com.archiveat.server.domain.collection.entity.Collection;
import com.archiveat.server.domain.collection.entity.CollectionNewsletter;
import com.archiveat.server.domain.collection.repository.CollectionRepository;
import com.archiveat.server.domain.home.dto.response.HomeResponse;
import com.archiveat.server.domain.newsletter.entity.Domain;
import com.archiveat.server.domain.newsletter.entity.Newsletter;
import com.archiveat.server.domain.newsletter.entity.UserNewsletter;
import com.archiveat.server.domain.newsletter.repository.UserNewsletterRepository;
import com.archiveat.server.global.common.constant.DepthType;
import com.archiveat.server.global.common.constant.HomeTabType;
import com.archiveat.server.global.common.constant.PerspectiveType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HomeServiceTest {

    @Mock
    private UserNewsletterRepository userNewsletterRepository;
    @Mock
    private CollectionRepository collectionRepository;

    @InjectMocks
    private HomeService homeService;

    @Test
    @DisplayName("[Home] 메인 데이터 조회 테스트")
    void getHomeData_Success() {
        // given
        Long userId = 1L;

        // 뉴스레터 Mock 생성
        List<Newsletter> mockNewsletters = new ArrayList<>();
        for (long i = 10; i < 14; i++) {
            Newsletter n = mock(Newsletter.class);
            when(n.getId()).thenReturn(i);
            when(n.getTitle()).thenReturn("Test Newsletter " + i);
            when(n.getThumbnailUrl()).thenReturn("http://example.com/" + i + ".jpg");
            when(n.getSmallCardSummary()).thenReturn("Small summary " + i);
            when(n.getMediumCardSummary()).thenReturn("Medium summary " + i);
            Domain mockDomain = mock(Domain.class);
            when(mockDomain.getName()).thenReturn("YouTube");
            when(n.getDomain()).thenReturn(mockDomain);
            mockNewsletters.add(n);
        }

        // UserNewsletter Mock 생성 및 연결
        List<UserNewsletter> mockUserNewsletters = mockNewsletters.stream()
                .map(n -> {
                    UserNewsletter un = mock(UserNewsletter.class);
                    long expectedId = n.getId() + 1000; // 임의의 UserNewsletter ID
                    when(un.getId()).thenReturn(expectedId);
                    when(un.getNewsletter()).thenReturn(n);
                    when(un.getPerspectiveType()).thenReturn(PerspectiveType.NOW);
                    when(un.getDepthType()).thenReturn(DepthType.LIGHT);
                    return un;
                }).collect(Collectors.toList());

        // Collection 관련 Mock 설정
        Collection mockCol = mock(Collection.class);
        when(mockCol.getId()).thenReturn(100L);
        when(mockCol.getTitle()).thenReturn("Test Collection");
        when(mockCol.getPerspectiveType()).thenReturn(PerspectiveType.NOW);
        when(mockCol.getDepthType()).thenReturn(DepthType.LIGHT);

        List<CollectionNewsletter> mockMapping = mockNewsletters.stream()
                .map(n -> {
                    CollectionNewsletter cn = mock(CollectionNewsletter.class);
                    when(cn.getNewsletter()).thenReturn(n);
                    return cn;
                }).collect(Collectors.toList());
        when(mockCol.getCollectionNewsletters()).thenReturn(mockMapping);

        // Mock 리포지토리 설정
        when(userNewsletterRepository.findAllByUserId(userId)).thenReturn(mockUserNewsletters);
        when(collectionRepository.findAllByUserId(userId)).thenReturn(List.of(mockCol));

        // when
        HomeResponse response = homeService.getHomeData(userId);

        // then
        assertSoftly(softly -> {
            // 인사말 & 탭 검증
            softly.assertThat(response.firstGreetingMessage()).matches("좋은 (아침|밤)이에요!");
            softly.assertThat(response.tabs()).hasSize(HomeTabType.values().length);

            // 뉴스레터 카드 검증
            softly.assertThat(response.contentCards().getFirst().userNewsletterId()).isGreaterThan(1000L);
            softly.assertThat(response.contentCards()).hasSize(4);
            softly.assertThat(response.contentCards().getFirst().tabLabel()).isEqualTo("영감수집");

            // 컬렉션 카드 검증
            softly.assertThat(response.contentCollectionCards()).hasSize(1);
            softly.assertThat(response.contentCollectionCards().getFirst().thumbnails()).hasSize(4);
            softly.assertThat(response.contentCollectionCards().getFirst().cardType()).isEqualTo("컬렉션");

            // 썸네일 정보 검증
            softly.assertThat(response.contentCollectionCards().getFirst().thumbnails())
                    .extracting(HomeResponse.ThumbnailInfo::thumbnailUrl)
                    .contains("http://example.com/10.jpg");
        });
    }

    @Test
    @DisplayName("[Home] thumbnailUrl이 null일 때 domainName이 정상 반환되는지 검증")
    void getHomeData_NullThumbnailUrl_DomainNameReturned() {
        // given
        Long userId = 1L;

        // thumbnailUrl이 null인 Naver News 뉴스레터
        Newsletter naverNewsletter = mock(Newsletter.class);
        when(naverNewsletter.getId()).thenReturn(20L);
        when(naverNewsletter.getTitle()).thenReturn("네이버 뉴스 기사");
        when(naverNewsletter.getThumbnailUrl()).thenReturn(null); // thumbnailUrl null
        when(naverNewsletter.getSmallCardSummary()).thenReturn("요약");
        when(naverNewsletter.getMediumCardSummary()).thenReturn("중형 요약");
        Domain naverDomain = mock(Domain.class);
        when(naverDomain.getName()).thenReturn("Naver News");
        when(naverNewsletter.getDomain()).thenReturn(naverDomain);

        // thumbnailUrl이 null인 Tistory 뉴스레터
        Newsletter tistoryNewsletter = mock(Newsletter.class);
        when(tistoryNewsletter.getId()).thenReturn(21L);
        when(tistoryNewsletter.getTitle()).thenReturn("티스토리 블로그");
        when(tistoryNewsletter.getThumbnailUrl()).thenReturn(null); // thumbnailUrl null
        when(tistoryNewsletter.getSmallCardSummary()).thenReturn("요약");
        when(tistoryNewsletter.getMediumCardSummary()).thenReturn("중형 요약");
        Domain tistoryDomain = mock(Domain.class);
        when(tistoryDomain.getName()).thenReturn("Tistory");
        when(tistoryNewsletter.getDomain()).thenReturn(tistoryDomain);

        // thumbnailUrl이 있는 YouTube 뉴스레터
        Newsletter youtubeNewsletter = mock(Newsletter.class);
        when(youtubeNewsletter.getId()).thenReturn(22L);
        when(youtubeNewsletter.getTitle()).thenReturn("유튜브 영상");
        when(youtubeNewsletter.getThumbnailUrl()).thenReturn("https://img.youtube.com/thumb.jpg");
        when(youtubeNewsletter.getSmallCardSummary()).thenReturn("요약");
        when(youtubeNewsletter.getMediumCardSummary()).thenReturn("중형 요약");
        Domain youtubeDomain = mock(Domain.class);
        when(youtubeDomain.getName()).thenReturn("YouTube");
        when(youtubeNewsletter.getDomain()).thenReturn(youtubeDomain);

        List<Newsletter> newsletters = List.of(naverNewsletter, tistoryNewsletter, youtubeNewsletter);

        // UserNewsletter Mock 생성
        List<UserNewsletter> mockUserNewsletters = new ArrayList<>();
        for (int i = 0; i < newsletters.size(); i++) {
            Newsletter n = newsletters.get(i);
            long newsletterId = n.getId(); // extract before using in another when()
            UserNewsletter un = mock(UserNewsletter.class);
            when(un.getId()).thenReturn(newsletterId + 1000);
            when(un.getNewsletter()).thenReturn(n);
            when(un.getPerspectiveType()).thenReturn(PerspectiveType.NOW);
            when(un.getDepthType()).thenReturn(DepthType.LIGHT);
            mockUserNewsletters.add(un);
        }

        // Collection Mock (컬렉션 카드에서도 null thumbnail + domainName 검증)
        Collection mockCol = mock(Collection.class);
        when(mockCol.getId()).thenReturn(200L);
        when(mockCol.getTitle()).thenReturn("Test Collection");
        when(mockCol.getPerspectiveType()).thenReturn(PerspectiveType.NOW);
        when(mockCol.getDepthType()).thenReturn(DepthType.LIGHT);

        List<CollectionNewsletter> mockMapping = newsletters.stream()
                .map(n -> {
                    CollectionNewsletter cn = mock(CollectionNewsletter.class);
                    when(cn.getNewsletter()).thenReturn(n);
                    return cn;
                }).collect(Collectors.toList());
        when(mockCol.getCollectionNewsletters()).thenReturn(mockMapping);

        when(userNewsletterRepository.findAllByUserId(userId)).thenReturn(mockUserNewsletters);
        when(collectionRepository.findAllByUserId(userId)).thenReturn(List.of(mockCol));

        // when
        HomeResponse response = homeService.getHomeData(userId);

        // then
        assertSoftly(softly -> {
            // 개별 카드: thumbnailUrl null + domainName 검증
            HomeResponse.ContentCardResponse naverCard = response.contentCards().get(0);
            softly.assertThat(naverCard.thumbnailUrl()).isNull();
            softly.assertThat(naverCard.domainName()).isEqualTo("Naver News");

            HomeResponse.ContentCardResponse tistoryCard = response.contentCards().get(1);
            softly.assertThat(tistoryCard.thumbnailUrl()).isNull();
            softly.assertThat(tistoryCard.domainName()).isEqualTo("Tistory");

            HomeResponse.ContentCardResponse youtubeCard = response.contentCards().get(2);
            softly.assertThat(youtubeCard.thumbnailUrl()).isEqualTo("https://img.youtube.com/thumb.jpg");
            softly.assertThat(youtubeCard.domainName()).isEqualTo("YouTube");

            // 컬렉션 카드: ThumbnailInfo 내부 검증
            List<HomeResponse.ThumbnailInfo> thumbnails = response.contentCollectionCards().getFirst().thumbnails();
            softly.assertThat(thumbnails).hasSize(3);

            // Naver News: thumbnailUrl null, domainName "Naver News"
            softly.assertThat(thumbnails.get(0).thumbnailUrl()).isNull();
            softly.assertThat(thumbnails.get(0).domainName()).isEqualTo("Naver News");

            // Tistory: thumbnailUrl null, domainName "Tistory"
            softly.assertThat(thumbnails.get(1).thumbnailUrl()).isNull();
            softly.assertThat(thumbnails.get(1).domainName()).isEqualTo("Tistory");

            // YouTube: thumbnailUrl 존재, domainName "YouTube"
            softly.assertThat(thumbnails.get(2).thumbnailUrl()).isEqualTo("https://img.youtube.com/thumb.jpg");
            softly.assertThat(thumbnails.get(2).domainName()).isEqualTo("YouTube");
        });
    }

}
