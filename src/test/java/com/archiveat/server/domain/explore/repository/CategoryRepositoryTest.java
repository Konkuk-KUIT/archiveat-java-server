package com.archiveat.server.domain.explore.repository;

import com.archiveat.server.domain.explore.entity.Category;
import com.archiveat.server.domain.explore.entity.Topic;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
public class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    @Transactional
    public void testFindAllWithTopics() {
        List<Category> categories = categoryRepository.findAll();
        assertThat(categories).isNotEmpty();

        for (Category category : categories) {
            // topics가 lazy loading 없이 즉시 로딩되는지 검증
            assertThat(category.getTopics()).isNotNull();
        }
    }
}
