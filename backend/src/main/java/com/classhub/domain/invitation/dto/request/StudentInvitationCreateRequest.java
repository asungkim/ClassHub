package com.classhub.domain.invitation.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record StudentInvitationCreateRequest(
        @NotBlank
        @Email
        @Size(max = 120)
        String targetEmail,

        @NotNull
        UUID studentProfileId
) {
    public String normalizedEmail() {
        return targetEmail.trim().toLowerCase();
    }
}
