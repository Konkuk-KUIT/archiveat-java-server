package com.archiveat.server.domain.collection.service;

import com.archiveat.server.domain.collection.entity.Collection;
import com.archiveat.server.domain.collection.repository.CollectionRepository;
import com.archiveat.server.domain.explore.entity.Topic;
import com.archiveat.server.domain.explore.repository.TopicRepository;
import com.archiveat.server.domain.explore.repository.UserTopicRepository;
import com.archiveat.server.domain.newsletter.entity.Newsletter;
import com.archiveat.server.domain.newsletter.entity.UserNewsletter;
import com.archiveat.server.domain.newsletter.repository.UserNewsletterRepository;
import com.archiveat.server.domain.user.entity.User;
import com.archiveat.server.domain.user.repository.UserRepository;
import com.archiveat.server.global.common.constant.DepthType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CollectionGeneratorServiceTest {

    @InjectMocks
    private CollectionGeneratorService collectionGeneratorService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserNewsletterRepository userNewsletterRepository;

    @Mock
    private CollectionRepository collectionRepository;

    @Mock
    private TopicRepository topicRepository;

    @Mock
    private UserTopicRepository userTopicRepository;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Test
    @DisplayName("컬렉션 생성 성공: 오전 6시에는 LIGHT(요약형) 컬렉션이 생성된다")
    void generateCollections_Morning_LightDepth() {
        // given
        LocalTime morningTime = LocalTime.of(6, 0);
        User user = User.builder().build();
        ReflectionTestUtils.setField(user, "id", 1L);

        Newsletter n1 = Newsletter.builder().build();
        ReflectionTestUtils.setField(n1, "id", 10L);
        n1.updateCategoryAndTopic("IT", "AI");
        Newsletter n2 = Newsletter.builder().build();
        ReflectionTestUtils.setField(n2, "id", 11L);
        n2.updateCategoryAndTopic("IT", "AI");

        UserNewsletter un1 = UserNewsletter.builder().newsletter(n1).build();
        UserNewsletter un2 = UserNewsletter.builder().newsletter(n2).build();

        given(userRepository.findAll()).willReturn(List.of(user));
        // 6시이므로 LIGHT 타입의 뉴스레터 후보군 조회
        given(userNewsletterRepository.findUncollectedNewsletters(1L, DepthType.LIGHT))
                .willReturn(List.of(un1, un2));
        given(topicRepository.findByName("AI")).willReturn(Optional.of(Topic.builder().name("AI").build()));

        // TransactionTemplate 모킹 (실행 시 콜백을 즉시 실행하도록 설정)
        given(transactionTemplate.execute(any())).willAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });

        // when
        collectionGeneratorService.generateCollectionsForTime(morningTime);

        // then
        // LIGHT 타입의 컬렉션이 저장되었는지 검증
        verify(collectionRepository, atLeastOnce()).save(argThat(c -> c.getDepthType() == DepthType.LIGHT));
    }

    @Test
    @DisplayName("생성 건너뜀: 같은 주제의 뉴스레터가 2개 미만이면 생성하지 않는다")
    void generateCollections_Skip_WhenSingleNewsletter() {
        // given
        LocalTime time = LocalTime.of(12, 0);
        User user = User.builder().build();
        ReflectionTestUtils.setField(user, "id", 1L);
        Newsletter n1 = Newsletter.builder().build();
        ReflectionTestUtils.setField(n1, "id", 10L);
        n1.updateCategoryAndTopic("IT", "AI");
        UserNewsletter un1 = UserNewsletter.builder().newsletter(n1).build();

        given(userRepository.findAll()).willReturn(List.of(user));
        given(userNewsletterRepository.findUncollectedNewsletters(anyLong(), any(DepthType.class)))
                .willReturn(List.of(un1)); // 단 1개만 존재

        given(transactionTemplate.execute(any())).willAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });

        // when
        collectionGeneratorService.generateCollectionsForTime(time);

        // then
        verify(collectionRepository, never()).save(any(Collection.class));
    }
}