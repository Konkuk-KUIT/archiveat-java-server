package com.archiveat.server.global.util;

import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.regex.Pattern;

/**
 * URL 도메인 분류기
 * 
 * URL을 분석하여 어떤 플랫폼(YouTube, 네이버 뉴스, 티스토리 등)인지 판별합니다.
 */
@Slf4j
public class DomainClassifier {

    // URL 패턴 정의
    private static final Pattern YOUTUBE_PATTERN = Pattern.compile(
            ".*(youtube\\.com/watch|youtu\\.be/).*", Pattern.CASE_INSENSITIVE);

    private static final Pattern NAVER_NEWS_PATTERN = Pattern.compile(
            ".*(news\\.naver\\.com|n\\.news\\.naver\\.com)/.*", Pattern.CASE_INSENSITIVE);

    private static final Pattern TISTORY_PATTERN = Pattern.compile(
            ".*\\.tistory\\.com/.*", Pattern.CASE_INSENSITIVE);

    private static final Pattern BRUNCH_PATTERN = Pattern.compile(
            ".*brunch\\.co\\.kr/@.*", Pattern.CASE_INSENSITIVE);

    /**
     * URL로부터 도메인 타입 판별
     * 
     * @param url 분석할 URL
     * @return DomainType 열거형
     */
    public static DomainType classify(String url) {
        if (url == null || url.trim().isEmpty()) {
            log.warn("Empty or null URL provided");
            return DomainType.UNKNOWN;
        }

        try {
            // URL 유효성 검증
            URI uri = new URI(url);
            String host = uri.getHost();

            if (host == null) {
                log.warn("Invalid URL format: {}", url);
                return DomainType.UNKNOWN;
            }

            // 패턴 매칭으로 도메인 분류
            if (YOUTUBE_PATTERN.matcher(url).matches()) {
                log.info("Classified as YOUTUBE: {}", url);
                return DomainType.YOUTUBE;
            }

            if (NAVER_NEWS_PATTERN.matcher(url).matches()) {
                log.info("Classified as NAVER_NEWS: {}", url);
                return DomainType.NAVER_NEWS;
            }

            if (TISTORY_PATTERN.matcher(url).matches()) {
                log.info("Classified as TISTORY: {}", url);
                return DomainType.TISTORY;
            }

            if (BRUNCH_PATTERN.matcher(url).matches()) {
                log.info("Classified as BRUNCH: {}", url);
                return DomainType.BRUNCH;
            }

            // 매칭되지 않으면 일반 웹사이트
            log.info("Classified as GENERAL: {}", url);
            return DomainType.GENERAL;

        } catch (Exception e) {
            log.error("Failed to classify URL: {}", url, e);
            return DomainType.UNKNOWN;
        }
    }

    /**
     * 도메인 타입 열거형
     */
    public enum DomainType {
        YOUTUBE("YouTube 영상"),
        NAVER_NEWS("네이버 뉴스"),
        TISTORY("티스토리 블로그"),
        BRUNCH("브런치"),
        GENERAL("일반 웹사이트"),
        UNKNOWN("알 수 없음");

        private final String description;

        DomainType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        /**
         * YouTube인지 확인
         */
        public boolean isYouTube() {
            return this == YOUTUBE;
        }

        /**
         * 네이버 뉴스인지 확인
         */
        public boolean isNaverNews() {
            return this == NAVER_NEWS;
        }

        /**
         * 티스토리인지 확인
         */
        public boolean isTistory() {return this == TISTORY;}

        /**
         * 웹 크롤링이 필요한 타입인지 확인
         * (티스토리, 브런치, 네이버 뉴스, 일반 웹)
         */
        public boolean needsWebCrawling() {
            return this == NAVER_NEWS || this == TISTORY ||
                    this == BRUNCH || this == GENERAL;
        }
    }
}
