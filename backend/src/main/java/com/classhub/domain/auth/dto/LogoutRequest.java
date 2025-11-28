package com.classhub.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(
        @NotBlank
        String refreshToken,
        boolean logoutAll
) {
}
