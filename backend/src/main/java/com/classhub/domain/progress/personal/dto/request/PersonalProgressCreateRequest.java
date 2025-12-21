package com.classhub.domain.progress.personal.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record PersonalProgressCreateRequest(
        @NotNull LocalDate date,
        @NotBlank String title,
        @NotBlank String content
) {
}
