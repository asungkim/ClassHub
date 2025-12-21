package com.classhub.domain.progress.personal.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record PersonalProgressComposeRequest(
        @NotNull UUID studentCourseRecordId,
        @NotNull LocalDate date,
        @NotBlank String title,
        @NotBlank String content
) {
}
