package com.archiveat.server.domain.newsletter.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GenerateNewsletterRequest {
    @NotBlank
    String contentUrl;
    String memo;
}
