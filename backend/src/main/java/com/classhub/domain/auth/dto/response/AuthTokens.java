package com.classhub.domain.auth.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record AuthTokens(
        UUID memberId,
        String accessToken,
        LocalDateTime accessTokenExpiresAt,
        String refreshToken,
        LocalDateTime refreshTokenExpiresAt
) {
}

