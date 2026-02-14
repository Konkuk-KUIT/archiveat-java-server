package com.archiveat.server.global.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 단축 URL의 리다이렉트를 따라가 최종 URL을 반환합니다.
 */
@Slf4j
@Component
public class UrlRedirectResolver {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(CONNECT_TIMEOUT)
            .build();

    public String resolveIfShortUrl(String url) {
        if (url == null || url.isBlank()) {
            return url;
        }

        if (!isShortNaverUrl(url)) {
            return url;
        }

        try {
            String resolved = sendHead(url);
            if (resolved != null) {
                return resolved;
            }
        } catch (Exception headError) {
            log.info("HEAD failed for short URL, trying GET: {}", url, headError);
        }

        try {
            String resolved = sendGet(url);
            if (resolved != null) {
                return resolved;
            }
        } catch (Exception getError) {
            log.warn("Failed to resolve short URL: {}", url, getError);
        }

        return url;
    }

    private String sendHead(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .timeout(REQUEST_TIMEOUT)
                .header("User-Agent", "Archiveat-Server")
                .build();
        HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        return response.uri() != null ? response.uri().toString() : null;
    }

    private String sendGet(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .GET()
                .timeout(REQUEST_TIMEOUT)
                .header("User-Agent", "Archiveat-Server")
                .build();
        HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        return response.uri() != null ? response.uri().toString() : null;
    }

    private boolean isShortNaverUrl(String url) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host == null) {
                return false;
            }
            String lowerHost = host.toLowerCase();
            return lowerHost.endsWith("naver.me");
        } catch (Exception e) {
            return false;
        }
    }
}
