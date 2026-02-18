package com.archiveat.server.domain.collection.service;

import com.archiveat.server.domain.collection.dto.response.CollectionDetailResponse;
import com.archiveat.server.domain.collection.entity.Collection;
import com.archiveat.server.domain.collection.entity.CollectionNewsletter;
import com.archiveat.server.domain.collection.repository.CollectionNewsletterRepository;
import com.archiveat.server.domain.collection.repository.CollectionRepository;
import com.archiveat.server.domain.explore.entity.Topic;
import com.archiveat.server.domain.newsletter.entity.Domain;
import com.archiveat.server.domain.newsletter.entity.Newsletter;
import com.archiveat.server.domain.newsletter.entity.UserNewsletter;
import com.archiveat.server.domain.newsletter.repository.UserNewsletterRepository;
import com.archiveat.server.domain.user.entity.User;
import com.archiveat.server.global.exception.CustomException;
import com.archiveat.server.global.common.response.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CollectionServiceTest {

    @InjectMocks
    private CollectionService collectionService;

    @Mock
    private CollectionRepository collectionRepository;

    @Mock
    private CollectionNewsletterRepository collectionNewsletterRepository;

    @Mock
    private UserNewsletterRepository userNewsletterRepository;

    @Test
    @DisplayName("컬렉션 상세 조회 성공: 권한 및 데이터 매핑 검증")
    void getCollectionDetail_Success() {
        // given
        Long userId = 1L;
        Long collectionId = 100L;

        User user = User.builder().nickname("아카이빗").build();
        ReflectionTestUtils.setField(user, "id", userId);

        Topic topic = Topic.builder().name("AI").build();
        ReflectionTestUtils.setField(topic, "id", 10L);

        Collection collection = Collection.builder().user(user).topic(topic).build();
        ReflectionTestUtils.setField(collection, "id", collectionId);

        Domain domain = Domain.builder().name("테크레터").build();

        Newsletter newsletter = Newsletter.builder()
                .domain(domain)
                .title("AI 소식")
                .contentUrl("url")
                .build();
        ReflectionTestUtils.setField(newsletter, "id", 200L);

        CollectionNewsletter cn = CollectionNewsletter.builder().collection(collection).newsletter(newsletter).build();
        ReflectionTestUtils.setField(cn, "id", 1L);

        // UserNewsletter 생성 및 시간 주입
        UserNewsletter un = UserNewsletter.builder().newsletter(newsletter).isRead(true).memo("꿀정보").build();
        ReflectionTestUtils.setField(un, "createdAt", LocalDateTime.now());

        given(collectionRepository.findById(collectionId)).willReturn(Optional.of(collection));
        given(collectionNewsletterRepository.findAllByCollectionId(collectionId)).willReturn(List.of(cn));
        given(userNewsletterRepository.findByUserIdAndNewsletterIdIn(userId, List.of(200L))).willReturn(List.of(un));

        // when
        CollectionDetailResponse response = collectionService.getCollectionDetail(userId, collectionId);

        // then
        assertThat(response.collectionInfo().topicName()).isEqualTo("AI");
        assertThat(response.newsletters()).hasSize(1);
        assertThat(response.newsletters().get(0).memo()).isEqualTo("꿀정보");
    }

    @Test
    @DisplayName("실패: 본인의 컬렉션이 아니면 FORBIDDEN 예외가 발생한다")
    void getCollectionDetail_Fail_Forbidden() {
        // given
        Long userId = 1L;
        Long otherUserId = 2L;
        Long collectionId = 100L;

        User otherUser = User.builder().build();
        ReflectionTestUtils.setField(otherUser, "id", otherUserId);

        Collection collection = Collection.builder().user(otherUser).build();
        ReflectionTestUtils.setField(collection, "id", collectionId);

        given(collectionRepository.findById(collectionId)).willReturn(Optional.of(collection));

        // when & then
        assertThatThrownBy(() -> collectionService.getCollectionDetail(userId, collectionId))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.FORBIDDEN.getMessage());
    }
}