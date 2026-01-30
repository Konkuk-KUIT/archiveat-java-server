package com.archiveat.server.global.common.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AvailabilityType {
    MORNING("아침"),
    LUNCHTIME("점심"),
    EVENING("저녁"),
    BEDTIME("자기 전");

    private final String description;
}
