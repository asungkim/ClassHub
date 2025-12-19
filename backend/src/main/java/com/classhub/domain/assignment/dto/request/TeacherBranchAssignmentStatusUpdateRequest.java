package com.classhub.domain.assignment.dto.request;

import jakarta.validation.constraints.NotNull;

public record TeacherBranchAssignmentStatusUpdateRequest(
        @NotNull Boolean enabled
) {
}
