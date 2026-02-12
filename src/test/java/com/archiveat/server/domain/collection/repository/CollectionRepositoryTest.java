package com.archiveat.server.domain.collection.repository;

import com.archiveat.server.domain.collection.entity.Collection;
import com.archiveat.server.domain.collection.entity.CollectionNewsletter;
import com.archiveat.server.domain.explore.entity.Category;
import com.archiveat.server.domain.explore.entity.Topic;
import com.archiveat.server.domain.newsletter.entity.Newsletter;
import com.archiveat.server.domain.user.entity.User;
import com.archiveat.server.global.common.constant.DepthType;
import com.archiveat.server.global.common.constant.PerspectiveType;
import com.archiveat.server.global.config.JpaConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

@DataJpaTest
@Import(JpaConfig.class)
public class CollectionRepositoryTest {

    @Autowired private CollectionRepository collectionRepository;
    @Autowired private CollectionNewsletterRepository collectionNewsletterRepository;
    @Autowired private TestEntityManager em;

    @Test
    @DisplayName("[Collection] 유저별 컬렉션 목록 및 내부 뉴스레터 조회 통합 검증")
    void findAllByUserId_Success() {
        // given
        User user = User.builder().email("test@test.com").nickname("아카이빗").build();
        em.persist(user);

        Category category = Category.builder().name("IT").build();
        em.persist(category);

        Topic topic = Topic.builder().name("AI").category(category).build();
        em.persist(topic);

        Collection collection = Collection.builder().user(user).topic(topic).title("컬렉션").perspectiveType(PerspectiveType.NOW).depthType(DepthType.LIGHT).build();
        em.persist(collection);

        for (int i = 1; i <= 4; i++) {
            Newsletter newsletter = Newsletter.builder()
                    .title("뉴스레터" + i)
                    .contentUrl("https://link.com/" + i)
                    .build();
            em.persist(newsletter);

            CollectionNewsletter cn = CollectionNewsletter.builder()
                    .collection(collection)
                    .newsletter(newsletter)
                    .build();
            em.persist(cn);
        }

        em.flush();
        em.clear();

        // when
        List<Collection> userCollections = collectionRepository.findAllByUserId(user.getId());
        List<CollectionNewsletter> collectionItems = collectionNewsletterRepository.findAllByCollectionId(collection.getId());

        // then
        assertSoftly(softly -> {
            // 1. 컬렉션 자체 검증
            softly.assertThat(userCollections).hasSize(1);
            softly.assertThat(userCollections.getFirst().getTitle()).isEqualTo("컬렉션");

            // 2. 컬렉션 내부 뉴스레터 연결 검증 (HomeService의 썸네일 로직 핵심)
            softly.assertThat(collectionItems).hasSize(4);
            softly.assertThat(collectionItems.getFirst().getNewsletter().getTitle()).contains("뉴스레터");
        });
    }
}
