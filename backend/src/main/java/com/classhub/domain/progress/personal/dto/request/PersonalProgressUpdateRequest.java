package com.classhub.domain.progress.personal.dto.request;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record PersonalProgressUpdateRequest(
        LocalDate date,
        @Size(max = 200) String title,
        String content
) {
}
