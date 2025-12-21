package com.classhub.domain.progress.course.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CourseProgressCreateRequest(
        @NotNull LocalDate date,
        @NotBlank String title,
        @NotBlank String content
) {
}
