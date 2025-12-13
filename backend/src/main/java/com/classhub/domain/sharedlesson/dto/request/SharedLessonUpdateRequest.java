package com.classhub.domain.sharedlesson.dto.request;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record SharedLessonUpdateRequest(
        LocalDate date,
        @Size(min = 1, max = 100) String title,
        @Size(min = 1, max = 4000) String content
) {
}
