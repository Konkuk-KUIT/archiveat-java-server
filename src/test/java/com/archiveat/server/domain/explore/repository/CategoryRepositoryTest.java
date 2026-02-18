package com.archiveat.server.domain.explore.repository;

import com.archiveat.server.domain.explore.entity.Category;
import com.archiveat.server.domain.explore.entity.Topic;
import com.archiveat.server.global.config.JpaConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConfig.class)
public class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private TopicRepository topicRepository;

    @Test
    @DisplayName("카테고리와 토픽 목록을 함께 조회한다")
    void findAllWithTopics_Success() {
        // given
        Category category = Category.builder().name("기술").build();
        categoryRepository.save(category);

        Topic topic = Topic.builder().name("AI").category(category).build();
        topicRepository.save(topic);

        category.getTopics().add(topic);

        // when
        List<Category> categories = categoryRepository.findAll();

        // then
        assertThat(categories).isNotEmpty();
        assertThat(categories.get(0).getTopics()).hasSize(1);
    }
}
