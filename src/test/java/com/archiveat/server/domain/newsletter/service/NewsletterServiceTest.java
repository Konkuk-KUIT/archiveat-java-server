package com.archiveat.server.domain.newsletter.service;

import com.archiveat.server.domain.newsletter.dto.response.SimpleViewNewsletterResponse;
import com.archiveat.server.domain.newsletter.entity.Newsletter;
import com.archiveat.server.domain.newsletter.entity.UserNewsletter;
import com.archiveat.server.domain.newsletter.repository.UserNewsletterRepository;
import com.archiveat.server.global.common.constant.DepthType;
import com.archiveat.server.global.common.constant.PerspectiveType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class NewsletterServiceTest {

    @InjectMocks
    private NewsletterService newsletterService;

    @Mock
    private UserNewsletterRepository userNewsletterRepository;

    @Spy
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("simpleViewUserNewsletter returns the first summary block")
    void simpleViewUserNewsletter_ReturnsFirstSummaryBlock() throws Exception {
        // given
        Long userId = 1L;
        Long userNewsletterId = 1L;
        String summaryJson = "[{\"title\":\"Summary 1\",\"content\":\"Content 1\"}, {\"title\":\"Summary 2\",\"content\":\"Content 2\"}]";

        Newsletter newsletter = new Newsletter(null, "http://example.com");
        ReflectionTestUtils.setField(newsletter, "newsletterSummary", summaryJson);
        ReflectionTestUtils.setField(newsletter, "title", "Newsletter Title");
        ReflectionTestUtils.setField(newsletter, "category", "Category");
        ReflectionTestUtils.setField(newsletter, "topic", "Topic");
        ReflectionTestUtils.setField(newsletter, "thumbnailUrl", "http://thumb.url");

        UserNewsletter userNewsletter = UserNewsletter.create(null, newsletter, null, null, "Memo");
        ReflectionTestUtils.setField(userNewsletter, "id", userNewsletterId);
        ReflectionTestUtils.setField(userNewsletter, "depthType", DepthType.DEEP);
        ReflectionTestUtils.setField(userNewsletter, "perspectiveType", PerspectiveType.NOW);

        given(userNewsletterRepository.findByIdAndUser_Id(userNewsletterId, userId))
                .willReturn(Optional.of(userNewsletter));

        // when
        SimpleViewNewsletterResponse response = newsletterService.simpleViewUserNewsletter(userId, userNewsletterId,
                true);

        // then
        assertThat(response.newsletterSimpleSummary()).hasSize(1);
        assertThat(response.newsletterSimpleSummary().get(0).title()).isEqualTo("Summary 1");
        assertThat(response.newsletterSimpleSummary().get(0).content()).isEqualTo("Content 1");
    }

    @Test
    @DisplayName("simpleViewUserNewsletter returns empty list when summary is empty")
    void simpleViewUserNewsletter_ReturnsEmptyList_WhenSummaryIsEmpty() throws Exception {
        // given
        Long userId = 1L;
        Long userNewsletterId = 1L;
        String summaryJson = "[]";

        Newsletter newsletter = new Newsletter(null, "http://example.com");
        ReflectionTestUtils.setField(newsletter, "newsletterSummary", summaryJson);
        ReflectionTestUtils.setField(newsletter, "title", "Newsletter Title");

        UserNewsletter userNewsletter = UserNewsletter.create(null, newsletter, null, null, "Memo");
        ReflectionTestUtils.setField(userNewsletter, "id", userNewsletterId);
        ReflectionTestUtils.setField(userNewsletter, "depthType", DepthType.DEEP);
        ReflectionTestUtils.setField(userNewsletter, "perspectiveType", PerspectiveType.NOW);

        given(userNewsletterRepository.findByIdAndUser_Id(userNewsletterId, userId))
                .willReturn(Optional.of(userNewsletter));

        // Real ObjectMapper to test actual parsing logic
        // ObjectMapper is not used when summary is "[]" because of the check in
        // parseNewsletterSummary
        // so we don't need to stub it here

        // when
        SimpleViewNewsletterResponse response = newsletterService.simpleViewUserNewsletter(userId, userNewsletterId,
                true);

        // then
        assertThat(response.newsletterSimpleSummary()).isEmpty();
    }
}
