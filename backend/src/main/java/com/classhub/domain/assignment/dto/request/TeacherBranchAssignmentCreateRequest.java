package com.classhub.domain.assignment.dto.request;

import com.classhub.domain.assignment.model.BranchRole;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record TeacherBranchAssignmentCreateRequest(
        @NotNull UUID branchId,
        BranchRole role
) {
}
