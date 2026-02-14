package com.archiveat.server.global.util;

import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 공유 URL을 실제 콘텐츠 URL로 정규화합니다.
 */
@Slf4j
public class UrlNormalizer {

    private UrlNormalizer() {
        // 유틸 클래스이므로 인스턴스화 방지
    }

    public static String normalize(String url) {
        if (url == null) {
            return null; // null은 그대로 반환
        }

        String normalized = url.trim(); // 앞뒤 공백 제거
        if (normalized.isEmpty()) {
            return normalized; // 빈 문자열은 그대로 반환
        }

        // 공유 URL이 중첩될 수 있어 최대 3회까지 언랩
        for (int i = 0; i < 3; i++) {
            String unwrapped = unwrapShareUrl(normalized); // 한 단계 언랩
            if (unwrapped.equals(normalized)) {
                break; // 더 이상 변환 없음
            }
            log.info("Unwrapped share URL: {} -> {}", normalized, unwrapped); // 변환 로그
            normalized = unwrapped; // 최신 값으로 갱신
        }

        return normalized; // 최종 결과 반환
    }

    private static String unwrapShareUrl(String url) {
        try {
            URI uri = new URI(url); // URI 파싱
            String host = uri.getHost(); // 호스트 추출
            if (host == null) {
                return url; // 호스트가 없으면 원본 반환
            }

            String lowerHost = host.toLowerCase(); // 도메인 소문자화
            if (lowerHost.endsWith("naver.com") || lowerHost.endsWith("naver.me")) {
                Map<String, String> params = parseQueryParams(uri.getRawQuery()); // 쿼리 파싱
                String target = firstNonEmpty(params.get("url"), params.get("u"),
                        params.get("targetUrl"), params.get("originalUrl"), params.get("link")); // 후보 파라미터 선택
                if (target != null) {
                    String decoded = decodeUrl(target); // 디코딩
                    if (looksLikeUrl(decoded)) {
                        return decoded; // URL 형태면 반환
                    }
                }
            }

            return url; // 처리 대상이 아니면 원본 반환
        } catch (Exception e) {
            log.warn("Failed to normalize URL: {}", url, e); // 실패 로그
            return url; // 예외 시 원본 반환
        }
    }

    private static Map<String, String> parseQueryParams(String rawQuery) {
        Map<String, String> params = new HashMap<>(); // 결과 맵
        if (rawQuery == null || rawQuery.isEmpty()) {
            return params; // 쿼리가 없으면 빈 맵 반환
        }

        String[] pairs = rawQuery.split("&"); // 파라미터 분리
        for (String pair : pairs) {
            if (pair.isEmpty()) {
                continue; // 빈 항목은 무시
            }
            int idx = pair.indexOf('='); // '=' 위치
            String key = idx > 0 ? pair.substring(0, idx) : pair; // key 추출
            String value = idx > 0 ? pair.substring(idx + 1) : ""; // value 추출
            params.put(key, value); // 맵 저장
        }
        return params; // 결과 반환
    }

    private static String decodeUrl(String value) {
        String decoded = URLDecoder.decode(value, StandardCharsets.UTF_8); // 1차 디코딩
        if (decoded.contains("%3A%2F%2F")) { // 여전히 인코딩된 '://'
            String decodedTwice = URLDecoder.decode(decoded, StandardCharsets.UTF_8); // 2차 디코딩
            if (looksLikeUrl(decodedTwice)) {
                return decodedTwice; // 2차 결과가 URL이면 반환
            }
        }
        return decoded; // 기본은 1차 디코딩 결과 반환
    }

    private static boolean looksLikeUrl(String value) {
        return value != null && (value.startsWith("http://") || value.startsWith("https://")); // 간단한 URL 판별
    }

    private static String firstNonEmpty(String... values) {
        if (values == null) {
            return null; // 입력이 없으면 null
        }
        for (String value : values) {
            if (value != null && !value.isEmpty()) {
                return value; // 첫 번째 유효 값 반환
            }
        }
        return null; // 모두 비어있으면 null
    }
}
