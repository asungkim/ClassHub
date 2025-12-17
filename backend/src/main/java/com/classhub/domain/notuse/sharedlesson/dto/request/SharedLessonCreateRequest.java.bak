package com.classhub.domain.sharedlesson.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record SharedLessonCreateRequest(
        @NotNull UUID courseId,
        LocalDate date,
        @NotBlank @Size(max = 100) String title,
        @NotBlank @Size(max = 4000) String content
) {
}
