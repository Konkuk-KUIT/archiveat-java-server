package com.archiveat.server.domain.home.service;

import com.archiveat.server.domain.collection.entity.Collection;
import com.archiveat.server.domain.collection.entity.CollectionNewsletter;
import com.archiveat.server.domain.collection.repository.CollectionNewsletterRepository;
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
    @Mock
    private CollectionNewsletterRepository collectionNewsletterRepository;

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

        // Mock 리포지토리 설정
        when(userNewsletterRepository.findAllByUserId(userId)).thenReturn(mockUserNewsletters);
        when(collectionRepository.findAllByUserId(userId)).thenReturn(List.of(mockCol));
        when(collectionNewsletterRepository.findAllByCollectionId(100L)).thenReturn(mockMapping);

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

}
