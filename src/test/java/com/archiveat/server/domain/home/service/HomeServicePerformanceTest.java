package com.archiveat.server.domain.home.service;

import com.archiveat.server.domain.collection.entity.Collection;
import com.archiveat.server.domain.collection.entity.CollectionNewsletter;
import com.archiveat.server.domain.collection.repository.CollectionNewsletterRepository;
import com.archiveat.server.domain.collection.repository.CollectionRepository;
import com.archiveat.server.domain.explore.entity.Category;
import com.archiveat.server.domain.explore.entity.Topic;
import com.archiveat.server.domain.explore.repository.CategoryRepository;
import com.archiveat.server.domain.explore.repository.TopicRepository;
import com.archiveat.server.domain.newsletter.entity.Domain;
import com.archiveat.server.domain.newsletter.entity.Newsletter;
import com.archiveat.server.domain.newsletter.repository.DomainRepository;
import com.archiveat.server.domain.newsletter.repository.NewsletterRepository;
import com.archiveat.server.domain.user.entity.User;
import com.archiveat.server.domain.user.repository.UserRepository;
import com.archiveat.server.global.common.constant.DepthType;
import com.archiveat.server.global.common.constant.PerspectiveType;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class HomeServicePerformanceTest {

    @Autowired
    private HomeService homeService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CollectionRepository collectionRepository;

    @Autowired
    private NewsletterRepository newsletterRepository;

    @Autowired
    private CollectionNewsletterRepository collectionNewsletterRepository;

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private EntityManager entityManager;

    private Long userId;

    @BeforeEach
    void setUp() {
        // Data Cleanup
        collectionNewsletterRepository.deleteAll();
        collectionRepository.deleteAll();
        // UserNewsletter is not used in "Collection Card" logic in HomeService (it uses
        // CollectionNewsletter),
        // but let's keep it clean.

        userRepository.deleteAll();
        newsletterRepository.deleteAll();
        topicRepository.deleteAll();
        categoryRepository.deleteAll();
        domainRepository.deleteAll();

        // 1. Create Metadata
        Category category = categoryRepository.save(Category.builder().name("Economy").build());
        Topic topic = topicRepository.save(new Topic("Stock", category));
        Domain domain = domainRepository.save(new Domain("naver.com"));

        // 2. Create User
        User user = userRepository.save(User.builder()
                .email("test@example.com")
                .nickname("Tester")
                .employmentType(com.archiveat.server.global.common.constant.EmploymentType.STUDENT)
                .build());
        this.userId = user.getId();

        // 3. Create Newsletters (40 items)
        List<Newsletter> newsletters = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            newsletters.add(newsletterRepository.save(Newsletter.builder()
                    .domain(domain)
                    .title("Newsletter " + i)
                    .contentUrl("http://example.com/" + i)
                    .build()));
        }

        // 4. Create Collections (4 items - Max allowed by Unique Constraint)
        PerspectiveType[] perspectives = PerspectiveType.values();
        DepthType[] depths = DepthType.values();

        int count = 0;
        for (PerspectiveType p : perspectives) {
            for (DepthType d : depths) {
                Collection collection = collectionRepository.save(Collection.builder()
                        .user(user)
                        .topic(topic)
                        .title("Collection " + count)
                        .perspectiveType(p)
                        .depthType(d)
                        .build());

                for (int j = 0; j < 4; j++) {
                    Newsletter newsletter = newsletters.get((count * 4) + j);
                    collectionNewsletterRepository.save(CollectionNewsletter.builder()
                            .collection(collection)
                            .newsletter(newsletter)
                            .build());
                }
                count++;
            }
        }

        // 5. Clear Hibernate Statistics
        Session session = entityManager.unwrap(Session.class);
        session.getSessionFactory().getStatistics().setStatisticsEnabled(true);
        session.getSessionFactory().getStatistics().clear();
    }

    @Test
    @DisplayName("Measure Query Count and Execution Time")
    @Transactional
    void measureQueryCountAndTime() {
        // Clear Persistence Context to force DB queries
        entityManager.flush();
        entityManager.clear();

        Session session = entityManager.unwrap(Session.class);
        Statistics statistics = session.getSessionFactory().getStatistics();
        statistics.clear();

        long startTime = System.currentTimeMillis();
        homeService.getHomeData(userId);
        long endTime = System.currentTimeMillis();

        long queryCount = statistics.getPrepareStatementCount();
        long executionTime = endTime - startTime;

        System.out.println("=========================================");
        System.out.println("Performance Metrics (Single Request)");
        System.out.println("Query Count: " + queryCount);
        System.out.println("Execution Time: " + executionTime + " ms");
        System.out.println("=========================================");

        // Baseline assertion: Ensure functionality works
        assertThat(queryCount).isGreaterThan(0);
    }

    @Test
    @DisplayName("Measure Throughput (TPS)")
    void measureThroughput() throws InterruptedException {
        int threadCount = 32;
        int requestCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(requestCount);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < requestCount; i++) {
            executorService.submit(() -> {
                try {
                    homeService.getHomeData(userId);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        long endTime = System.currentTimeMillis();

        long totalTimeMs = endTime - startTime;
        double tps = (requestCount * 1000.0) / totalTimeMs;

        System.out.println("=========================================");
        System.out.println("Performance Metrics (Concurrency)");
        System.out.println("Threads: " + threadCount);
        System.out.println("Total Requests: " + requestCount);
        System.out.println("Total Time: " + totalTimeMs + " ms");
        System.out.println("Throughput (TPS): " + String.format("%.2f", tps));
        System.out.println("=========================================");
    }
}
