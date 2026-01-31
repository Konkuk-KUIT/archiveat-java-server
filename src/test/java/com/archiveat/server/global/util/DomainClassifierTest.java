package com.archiveat.server.global.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * DomainClassifier 테스트
 */
class DomainClassifierTest {

    @Test
    void testYouTubeUrls() {
        // YouTube 일반 URL
        assertEquals(DomainClassifier.DomainType.YOUTUBE,
                DomainClassifier.classify("https://www.youtube.com/watch?v=4I8fWk0k7Y8"));

        // YouTube 단축 URL
        assertEquals(DomainClassifier.DomainType.YOUTUBE,
                DomainClassifier.classify("https://youtu.be/4I8fWk0k7Y8"));
    }

    @Test
    void testNaverNewsUrls() {
        // 네이버 뉴스 일반 URL
        assertEquals(DomainClassifier.DomainType.NAVER_NEWS,
                DomainClassifier.classify(
                        "https://news.naver.com/main/read.nhn?mode=LSD&mid=sec&sid1=105&oid=001&aid=0012345678"));

        // 네이버 뉴스 모바일 URL
        assertEquals(DomainClassifier.DomainType.NAVER_NEWS,
                DomainClassifier.classify("https://n.news.naver.com/mnews/article/629/0000461258"));
    }

    @Test
    void testTistoryUrls() {
        assertEquals(DomainClassifier.DomainType.TISTORY,
                DomainClassifier.classify("https://example.tistory.com/123"));
    }

    @Test
    void testBrunchUrls() {
        assertEquals(DomainClassifier.DomainType.BRUNCH,
                DomainClassifier.classify("https://brunch.co.kr/@username/123"));
    }

    @Test
    void testGeneralUrls() {
        // 기타 일반 웹사이트
        assertEquals(DomainClassifier.DomainType.GENERAL,
                DomainClassifier.classify("https://www.example.com/article/123"));
    }

    @Test
    void testInvalidUrls() {
        assertEquals(DomainClassifier.DomainType.UNKNOWN,
                DomainClassifier.classify(""));

        assertEquals(DomainClassifier.DomainType.UNKNOWN,
                DomainClassifier.classify(null));

        assertEquals(DomainClassifier.DomainType.UNKNOWN,
                DomainClassifier.classify("invalid-url"));
    }

    @Test
    void testDomainTypeHelpers() {
        assertTrue(DomainClassifier.DomainType.YOUTUBE.isYouTube());
        assertFalse(DomainClassifier.DomainType.NAVER_NEWS.isYouTube());

        assertTrue(DomainClassifier.DomainType.NAVER_NEWS.isNaverNews());
        assertTrue(DomainClassifier.DomainType.NAVER_NEWS.needsWebCrawling());

        assertTrue(DomainClassifier.DomainType.TISTORY.needsWebCrawling());
        assertTrue(DomainClassifier.DomainType.BRUNCH.needsWebCrawling());
        assertTrue(DomainClassifier.DomainType.GENERAL.needsWebCrawling());

        assertFalse(DomainClassifier.DomainType.YOUTUBE.needsWebCrawling());
    }
}
