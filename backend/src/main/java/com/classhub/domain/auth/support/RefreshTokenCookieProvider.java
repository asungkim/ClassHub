package com.classhub.domain.auth.support;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import static org.springframework.http.ResponseCookie.*;

@Component
public class RefreshTokenCookieProvider {

    public static final String COOKIE_NAME = "refreshToken";

    @Value("${security.cookie.refresh.secure:false}")
    private boolean secure;

    @Value("${security.cookie.refresh.path:/}")
    private String path;

    @Value("${security.cookie.refresh.same-site:Lax}")
    private String sameSite;

    @Value("${security.cookie.refresh.domain:}")
    private String domain;

    public void setRefreshToken(HttpServletResponse response, String refreshToken, LocalDateTime expiresAt) {
        Duration duration = Duration.between(LocalDateTime.now(ZoneOffset.UTC), expiresAt);
        System.out.println("duration = " + duration.toSeconds());
        long maxAge = Math.max(duration.toSeconds(), 0);
        ResponseCookie cookie = baseCookieBuilder(refreshToken)
                .maxAge(maxAge)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    public void clearRefreshToken(HttpServletResponse response) {
        ResponseCookie cookie = baseCookieBuilder("")
                .maxAge(0)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    public Optional<String> extractRefreshToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    private ResponseCookieBuilder baseCookieBuilder(String value) {
        ResponseCookieBuilder builder = from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secure)
                .path(path)
                .sameSite(sameSite);
        if (domain != null && !domain.isBlank()) {
            builder.domain(domain);
        }
        return builder;
    }
}

