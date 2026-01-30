package com.archiveat.server.domain.newsletter.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GenerateNewsletterRequest {
    @NotBlank
    String contentUrl;
    String memo;
}
