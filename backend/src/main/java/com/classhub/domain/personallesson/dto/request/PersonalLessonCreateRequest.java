package com.classhub.domain.personallesson.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record PersonalLessonCreateRequest(
        @NotNull UUID studentProfileId,
        @NotNull LocalDate date,
        @NotBlank @Size(max = 2000) String content
) {
}
