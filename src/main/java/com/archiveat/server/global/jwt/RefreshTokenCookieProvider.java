package com.archiveat.server.global.jwt;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenCookieProvider {

    private static final String COOKIE_NAME = "refreshToken";
    private final int maxAgeSeconds;

    public RefreshTokenCookieProvider(@Value("${jwt.refresh-token-expiration-ms}") long refreshTokenExpMs) {
        this.maxAgeSeconds = (int) (refreshTokenExpMs / 1000);
    }

    public void set(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie(COOKIE_NAME, refreshToken);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(maxAgeSeconds);

        // 운영 HTTPS면 true, 로컬 http 개발이면 false
        cookie.setSecure(false);

        response.addCookie(cookie);
    }

    public String extract(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if (COOKIE_NAME.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }

    public void clear(HttpServletResponse response) {
        Cookie cookie = new Cookie(COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setSecure(false);
        response.addCookie(cookie);
    }
}
