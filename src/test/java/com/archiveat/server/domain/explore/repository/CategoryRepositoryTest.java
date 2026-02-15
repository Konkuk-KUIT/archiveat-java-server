package com.archiveat.server.domain.explore.repository;

import com.archiveat.server.domain.explore.entity.Category;
import com.archiveat.server.domain.explore.entity.Topic;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

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
        System.out.println("=== Category Data Debug ===");
        for (Category category : categories) {
            System.out.println("Category: " + category.getName() + " (ID: " + category.getId() + ")");
            List<Topic> topics = category.getTopics();
            System.out.println("  Topics count: " + topics.size());
            for (Topic topic : topics) {
                System.out.println("    - Topic: " + topic.getName() + " (ID: " + topic.getId() + ")");
            }
        }
        System.out.println("===========================");
    }
}
