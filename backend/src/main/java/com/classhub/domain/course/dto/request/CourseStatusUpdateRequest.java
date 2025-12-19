package com.classhub.domain.course.dto.request;

import jakarta.validation.constraints.NotNull;

public record CourseStatusUpdateRequest(
        @NotNull Boolean enabled
) {
}
