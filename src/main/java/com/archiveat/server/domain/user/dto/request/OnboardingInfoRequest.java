package com.archiveat.server.domain.user.dto.request;

import com.archiveat.server.global.common.constant.DepthType;
import com.archiveat.server.global.common.constant.EmploymentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record OnboardingInfoRequest(
        @NotNull(message = "직업 군 정보는 필수입니다.")
        EmploymentType employmentType,

        @Valid
        @NotNull(message = "시간대별 선호도 정보는 필수입니다.")
        AvailabilityRequest availability,

        @Valid
        @NotNull(message = "관심사 정보는 필수입니다.")
        List<CategoryInterestRequest> interests // InterestsRequest 제거하고 바로 List로 받음
) {
    public record AvailabilityRequest(
            DepthType pref_morning,
            DepthType pref_lunch,
            DepthType pref_evening,
            DepthType pref_bedtime
    ) {}

    public record CategoryInterestRequest(
            @NotNull Long categoryId,
            List<Long> topicIds
    ) {}
}
