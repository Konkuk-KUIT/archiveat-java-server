package com.archiveat.server.domain.report.repository;

import com.archiveat.server.domain.explore.entity.Category;
import com.archiveat.server.domain.explore.entity.Topic;
import com.archiveat.server.domain.report.entity.Report;
import com.archiveat.server.domain.report.entity.TopicReport;
import com.archiveat.server.domain.user.entity.User;
import com.archiveat.server.global.config.JpaConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaConfig.class)
class ReportRepositoryTest {

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private TopicReportRepository topicReportRepository;

    @Autowired
    private TestEntityManager em;

    @Test
    @DisplayName("성공: 주간 리포트 및 토픽별 상세 비중 저장 검증")
    void saveReportAndTopicReport_Success() {
        // given
        User user = User.builder().email("report@test.com").nickname("리포트유저").build();
        em.persist(user);

        Category category = Category.builder().name("기술").build();
        em.persist(category);

        Topic topic = Topic.builder().name("AI").category(category).build();
        em.persist(topic);

        // 1. 리포트 본체 생성 (Builder 사용)
        Report report = Report.builder()
                .user(user)
                .totalNewsletterCount(10)
                .consumedNewsletterCount(5)
                .deepConsumedBalance(new BigDecimal("0.30"))
                .lightConsumedBalance(new BigDecimal("0.70"))
                .nowConsumedBalance(new BigDecimal("0.60"))
                .futureConsumedBalance(new BigDecimal("0.40"))
                .build();

        Report savedReport = reportRepository.save(report);

        // 2. 토픽 리포트 상세 생성 및 연결
        TopicReport topicReport = TopicReport.builder()
                .report(savedReport)
                .topic(topic)
                .consumeBalance(new BigDecimal("0.50"))
                .build();

        topicReportRepository.save(topicReport);

        em.flush();
        em.clear();

        // when
        Report foundReport = reportRepository.findById(savedReport.getId()).orElseThrow();
        List<TopicReport> foundTopicReports = topicReportRepository.findAll(); // 나중에 쿼리 메서드 추가 시 교체 가능

        // then
        assertThat(foundReport.getTotalNewsletterCount()).isEqualTo(10);
        assertThat(foundReport.getUser().getId()).isEqualTo(user.getId());

        assertThat(foundTopicReports).hasSize(1);
        assertThat(foundTopicReports.getFirst().getTopic().getName()).isEqualTo("AI");
        assertThat(foundTopicReports.getFirst().getReport().getId()).isEqualTo(foundReport.getId());
    }
}