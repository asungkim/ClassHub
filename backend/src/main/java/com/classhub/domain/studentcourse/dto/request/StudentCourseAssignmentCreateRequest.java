package com.classhub.domain.studentcourse.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record StudentCourseAssignmentCreateRequest(
        @NotNull UUID studentId,
        @NotNull UUID courseId
) {
}
