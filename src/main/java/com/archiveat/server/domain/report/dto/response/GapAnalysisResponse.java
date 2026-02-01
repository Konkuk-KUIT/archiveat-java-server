package com.archiveat.server.domain.report.dto.response;

import java.util.List;

public record GapAnalysisResponse(
        List<TopicGap> topics) {
    public record TopicGap(
            Long id, // topic id
            String name, // topic name
            Integer savedCount,
            Integer readCount) {
    }
}
