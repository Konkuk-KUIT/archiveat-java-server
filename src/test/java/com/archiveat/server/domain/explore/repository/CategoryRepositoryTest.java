package com.archiveat.server.domain.explore.repository;

import com.archiveat.server.domain.explore.entity.Category;
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

    @Test
    @DisplayName("카테고리와 토픽 목록을 함께 조회한다")
    public void testFindAllWithTopics() {
        Category category = Category.builder()
                .name("테스트 카테고리")
                .build();
        categoryRepository.save(category);

        List<Category> categories = categoryRepository.findAll();

        assertThat(categories).isNotEmpty();
        assertThat(categories.getFirst().getName()).isEqualTo("테스트 카테고리");

        for (Category c : categories) {
            assertThat(c.getTopics()).isNotNull();
        }
    }
}
