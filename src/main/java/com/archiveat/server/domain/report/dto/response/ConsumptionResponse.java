package com.archiveat.server.domain.report.dto.response;

import java.time.LocalDate;
import java.util.List;

public record ConsumptionResponse(
        Integer totalSavedCount,
        Integer totalReadCount,
        List<RecentRead> recentReadNewsletters) {
    public record RecentRead(
            Long id, // newsletter id
            String title,
            String categoryName, // Topic의 Category name
            LocalDate lastViewedAt) {
    }
}
