package com.classhub.domain.invitation.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AssistantInvitationCreateRequest(
        @NotBlank
        @Email
        @Size(max = 120)
        String targetEmail
) {

    public String normalizedEmail() {
        return targetEmail.trim().toLowerCase();
    }
}
