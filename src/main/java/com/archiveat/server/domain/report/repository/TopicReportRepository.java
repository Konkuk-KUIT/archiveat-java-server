package com.archiveat.server.domain.report.repository;

import com.archiveat.server.domain.report.entity.TopicReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TopicReportRepository extends JpaRepository<TopicReport, Long> {
}
