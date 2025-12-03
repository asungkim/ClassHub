package com.classhub.domain.personallesson.dto.request;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record PersonalLessonUpdateRequest(
        LocalDate date,
        @Size(max = 2000) String content
) {
}
