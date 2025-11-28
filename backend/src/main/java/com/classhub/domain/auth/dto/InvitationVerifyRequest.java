package com.classhub.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record InvitationVerifyRequest(
        @NotBlank
        String code
) {
}
