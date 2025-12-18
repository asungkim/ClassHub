package com.classhub.domain.invitation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InvitationVerifyRequest(
        @NotBlank
        @Size(min = 8, max = 64)
        String code
) {

    public String trimmedCode() {
        return code.trim();
    }
}
