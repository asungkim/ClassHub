package com.classhub.domain.invitation.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record AssistantInvitationCreateRequest(
        @NotBlank
        @Email
        @Size(max = 120)
        String targetEmail,

        @Future
        LocalDateTime expiredAt
) {

    public String normalizedTargetEmail() {
        return targetEmail.trim().toLowerCase();
    }
}
