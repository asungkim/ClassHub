package com.classhub.domain.auth.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record InvitationVerifyResponse(
        UUID inviterId,
        String inviterName,
        String inviteeRole,
        LocalDateTime expiresAt
) {
}
