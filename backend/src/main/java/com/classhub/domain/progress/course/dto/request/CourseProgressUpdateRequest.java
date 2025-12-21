package com.classhub.domain.progress.course.dto.request;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CourseProgressUpdateRequest(
        LocalDate date,
        @Size(max = 200) String title,
        String content
) {
}
