package com.archiveat.server.global.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * 단축 URL의 리다이렉트를 따라가 최종 URL을 반환합니다.
 */
@Slf4j
@Component
public class UrlRedirectResolver {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);
    private static final int MAX_REDIRECTS = 5;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
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
        return followRedirects(url, true);
    }

    private String sendGet(String url) throws Exception {
        return followRedirects(url, false);
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

    private String followRedirects(String url, boolean headOnly) throws Exception {
        URI current = URI.create(url);
        if (!isSafeHttpUrl(current)) {
            throw new IllegalArgumentException("Blocked unsafe URL: " + url);
        }

        for (int i = 0; i < MAX_REDIRECTS; i++) {
            HttpRequest.Builder builder = HttpRequest.newBuilder(current)
                    .timeout(REQUEST_TIMEOUT)
                    .header("User-Agent", "Archiveat-Server");
            if (headOnly) {
                builder.method("HEAD", HttpRequest.BodyPublishers.noBody());
            } else {
                builder.GET();
            }

            HttpResponse<Void> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.discarding());
            int status = response.statusCode();
            if (status >= 300 && status < 400) {
                String location = firstHeader(response, "location");
                if (location == null || location.isBlank()) {
                    return current.toString();
                }
                URI next = current.resolve(location);
                if (!isSafeHttpUrl(next)) {
                    throw new IllegalArgumentException("Blocked unsafe redirect URL: " + next);
                }
                current = next;
                continue;
            }
            return response.uri() != null ? response.uri().toString() : current.toString();
        }

        throw new IllegalStateException("Too many redirects: " + url);
    }

    private String firstHeader(HttpResponse<?> response, String name) {
        List<String> values = response.headers().allValues(name);
        return values.isEmpty() ? null : values.get(0);
    }

    private boolean isSafeHttpUrl(URI uri) {
        if (uri == null) {
            return false;
        }
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            return false;
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            return false;
        }
        if (host.equalsIgnoreCase("localhost")) {
            return false;
        }

        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress address : addresses) {
                if (isBlockedAddress(address)) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            log.warn("Failed to resolve host for SSRF check: {}", host, e);
            return false;
        }
    }

    private boolean isBlockedAddress(InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()) {
            return true;
        }
        String ip = address.getHostAddress();
        if (ip == null) {
            return true;
        }
        String lower = ip.toLowerCase();
        if (lower.startsWith("fc") || lower.startsWith("fd")) {
            return true; // IPv6 ULA (fc00::/7)
        }
        return false;
    }
}
