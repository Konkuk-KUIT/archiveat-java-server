package com.archiveat.server.global.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UrlNormalizerTest {

    @Test
    void unwrapsNaverShareUrlToTarget() {
        String input = "https://share.naver.com/web/shareView?url=https%3A%2F%2Fexample.tistory.com%2F123&title=test";
        String expected = "https://example.tistory.com/123";
        assertEquals(expected, UrlNormalizer.normalize(input));
    }

    @Test
    void unwrapsNaverBridgeUrlToTarget() {
        String input = "https://link.naver.com/bridge?url=https%3A%2F%2Fluluj-australia.tistory.com%2Fm%2F451&dst=naversearchapp%3A%2F%2Finappbrowser";
        String expected = "https://luluj-australia.tistory.com/m/451";
        assertEquals(expected, UrlNormalizer.normalize(input));
    }

    @Test
    void keepsRegularUrl() {
        String input = "https://example.tistory.com/123";
        assertEquals(input, UrlNormalizer.normalize(input));
    }
}
