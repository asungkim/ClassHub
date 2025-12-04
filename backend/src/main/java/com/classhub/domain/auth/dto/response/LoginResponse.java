package com.classhub.domain.auth.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record LoginResponse(
        UUID memberId,
        String accessToken,
        LocalDateTime accessTokenExpiresAt
) {

    public static LoginResponse from(AuthTokens tokens) {
        return new LoginResponse(
                tokens.memberId(),
                tokens.accessToken(),
                tokens.accessTokenExpiresAt()
        );
    }
}
