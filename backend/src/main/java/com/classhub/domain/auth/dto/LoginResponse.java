package com.classhub.domain.auth.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record LoginResponse(
        UUID memberId,
        String accessToken,
        String refreshToken,
        LocalDateTime accessTokenExpiresAt,
        LocalDateTime refreshTokenExpiresAt
) {
}
