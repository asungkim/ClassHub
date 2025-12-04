package com.classhub.domain.auth.dto.request;

public record LogoutRequest(
        String refreshToken,
        boolean logoutAll
) {
}
